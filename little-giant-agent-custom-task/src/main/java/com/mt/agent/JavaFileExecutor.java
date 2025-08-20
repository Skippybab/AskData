package com.mt.agent;

import javax.tools.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Java代码字符串动态编译和执行器
 * 可以动态编译并执行Java代码字符串
 */
public class JavaFileExecutor {

    // 依赖缓存，用于存储可能注入的依赖对象
    private final Map<Class<?>, Object> dependencyCache = new HashMap<>();

    /**
     * 添加依赖对象到缓存中，用于后续注入
     *
     * @param dependency 依赖对象
     */
    public <T> void addDependency(T dependency) {
        if (dependency != null) {
            dependencyCache.put(dependency.getClass(), dependency);
            // 同时添加接口类型，便于按接口注入
            for (Class<?> iface : dependency.getClass().getInterfaces()) {
                dependencyCache.put(iface, dependency);
            }
        }
    }

    /**
     * 编译并执行Java代码字符串
     *
     * @param javaCode   Java代码字符串
     * @param methodName 要调用的方法名
     * @param args       方法参数
     * @return 方法执行结果
     * @throws Exception 编译或执行过程中的异常
     */
    public void executeJavaCode(String javaCode, String methodName, Object... args) throws Exception {
        // 1. 从代码中提取类名和包名
        String className = extractClassName(javaCode);
        String packageName = extractPackageName(javaCode);
        String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;

        // 2. 创建临时目录存放编译结果
        File tempDir = createTempDirectory();

        try {
            // 3. 编译代码
            compileJavaCode(javaCode, className, tempDir);

            // 4. 加载并实例化编译后的类
            URLClassLoader classLoader = new URLClassLoader(new URL[] { tempDir.toURI().toURL() });
            Class<?> compiledClass = classLoader.loadClass(fullClassName);
            Object instance = createInstance(compiledClass);

            // 4.1 注入依赖
            instance = injectDependencies(instance);

            // 5. 查找并调用指定方法
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }

            Method method = findMethod(compiledClass, methodName, paramTypes);
            if (method == null) {
                throw new NoSuchMethodException("未找到方法: " + methodName);
            }

            // 6. 执行方法并返回结果
            method.invoke(instance, args);
        } finally {
            // 7. 清理临时文件
            deleteDirectory(tempDir);
        }
    }

    /**
     * 执行返回Flowable的Java代码，并对流中的每个元素应用消费者函数
     *
     * @param javaCode   Java代码字符串
     * @param methodName 要调用的方法名
     * @param consumer   处理流中每个元素的消费者函数
     * @param args       方法参数
     * @throws Exception 编译或执行过程中的异常
     */
    public void executeJavaCode(String javaCode, String methodName, Consumer<String> consumer, Object... args)
            throws Exception {
        // 1. 从代码中提取类名和包名
        String className = extractClassName(javaCode);
        String packageName = extractPackageName(javaCode);
        String fullClassName = packageName.isEmpty() ? className : packageName + "." + className;

        // 2. 创建临时目录存放编译结果
        File tempDir = createTempDirectory();

        try {
            // 3. 编译代码
            compileJavaCode(javaCode, className, tempDir);

            // 4. 创建类加载器并加载类
            URLClassLoader classLoader = new URLClassLoader(new URL[] { tempDir.toURI().toURL() },
                    Thread.currentThread().getContextClassLoader());
            Class<?> compiledClass = classLoader.loadClass(fullClassName);
            Object instance = createInstance(compiledClass);

            // 4.1 注入依赖
            instance = injectDependencies(instance);

            // 5. 查找并调用指定方法
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }

            Method method = findMethod(compiledClass, methodName, paramTypes);
            if (method == null) {
                throw new NoSuchMethodException("未找到方法: " + methodName);
            }

            // 6. 执行方法获取结果
            Object result = method.invoke(instance, args);

            // 7. 根据返回值类型进行不同处理
            if (result == null) {
                consumer.accept("方法执行完成，返回值为null");
                return;
            }

            // 检查返回值类型，只对流式对象调用subscribe
            String resultClassName = result.getClass().getName();

            if (resultClassName.contains("Flowable") || resultClassName.contains("Observable")) {
                // 处理RxJava流式对象
                handleReactiveStream(result, consumer, classLoader);
            } else if (result instanceof String) {
                // 直接处理String结果
                consumer.accept((String) result);
            } else {
                // 处理其他类型的结果
                consumer.accept("执行结果: " + result.toString());
            }

        } catch (Exception e) {
            consumer.accept("执行异常: " + e.getMessage());
            throw e;
        } finally {
            // 8. 清理临时文件
            deleteDirectory(tempDir);
        }
    }

    /**
     * 创建类的实例，尝试使用无参构造函数，如果失败则尝试注入依赖
     */
    private Object createInstance(Class<?> clazz) throws Exception {
        try {
            // 首先尝试使用无参构造函数
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            // 如果没有无参构造函数，尝试查找所有构造函数
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                Object[] params = new Object[paramTypes.length];
                boolean canUseConstructor = true;

                // 尝试从依赖缓存中获取所需参数
                for (int i = 0; i < paramTypes.length; i++) {
                    Object dependency = dependencyCache.get(paramTypes[i]);
                    if (dependency == null) {
                        canUseConstructor = false;
                        break;
                    }
                    params[i] = dependency;
                }

                if (canUseConstructor) {
                    return constructor.newInstance(params);
                }
            }

            // 如果无法满足任何构造函数的依赖，尝试创建模拟对象
            return null;
        }
    }

    /**
     * 为对象注入依赖
     * 处理@Autowired注解的字段
     *
     * @param instance 要注入依赖的对象实例
     * @return 注入依赖后的对象实例
     */
    private Object injectDependencies(Object instance) {
        if (instance == null) {
            return null;
        }

        Class<?> clazz = instance.getClass();

        // 获取所有声明的字段
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                // 检查字段是否带有@Autowired注解
                Class<? extends Annotation> autowiredClass = (Class<? extends Annotation>) getAnnotationClass(
                        "org.springframework.beans.factory.annotation.Autowired");

                // 如果带有@Autowired注解，或者是通过Spring管理的字段
                if ((autowiredClass != null && field.isAnnotationPresent(autowiredClass)) ||
                        field.getName().endsWith("Util") || field.getName().endsWith("Service")) {

                    // 从依赖缓存中查找匹配的依赖
                    Object dependency = dependencyCache.get(field.getType());

                    // 如果找到匹配的依赖，注入它
                    if (dependency != null) {
                        field.setAccessible(true);
                        field.set(instance, dependency);
                        System.out.println("成功注入依赖: " + field.getName() + " 到 " + clazz.getSimpleName());
                    } else {
                        System.out.println("警告: 无法找到类型为 " + field.getType().getName() + " 的依赖用于字段 " + field.getName());
                    }
                }
            } catch (Exception e) {
                System.out.println("注入依赖时发生异常: " + e.getMessage());
            }
        }

        return instance;
    }

    /**
     * 安全获取注解类，避免ClassNotFoundException
     */
    private Class<?> getAnnotationClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 从Java代码中提取类名
     */
    private String extractClassName(String javaCode) {
        // 查找类声明
        String classPattern = "public\\s+class\\s+(\\w+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(classPattern);
        java.util.regex.Matcher matcher = pattern.matcher(javaCode);

        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("无法从代码中提取类名");
    }

    /**
     * 从Java代码中提取包名
     */
    private String extractPackageName(String javaCode) {
        // 查找包声明
        String packagePattern = "package\\s+([\\w\\.]+);";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(packagePattern);
        java.util.regex.Matcher matcher = pattern.matcher(javaCode);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return ""; // 如果没有包声明，返回空字符串
    }

    /**
     * 创建临时目录
     */
    private File createTempDirectory() throws IOException {
        File tempDir = File.createTempFile("javaexecutor", "");
        tempDir.delete();
        tempDir.mkdir();
        return tempDir;
    }

    /**
     * 编译Java代码
     */
    private void compileJavaCode(String javaCode, String className, File outputDir) throws Exception {
        // 获取Java编译器
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("无法获取Java编译器，请确保JDK已正确安装");
        }

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);

        // 创建内存中的Java源文件
        JavaSourceObject sourceObject = new JavaSourceObject(className, javaCode);

        // 设置编译选项
        Iterable<String> options = Arrays.asList("-d", outputDir.getAbsolutePath());

        // 执行编译
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null,
                Collections.singletonList(sourceObject));

        boolean success = task.call();
        fileManager.close();

        if (!success) {
            throw new Exception("编译失败");
        }
    }

    /**
     * 根据方法名和参数类型查找匹配的方法
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        // 尝试直接获取方法
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            // 如果直接获取失败，尝试查找兼容的方法
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && isCompatible(method.getParameterTypes(), paramTypes)) {
                    return method;
                }
            }
            return null;
        }
    }

    /**
     * 检查方法参数类型是否兼容
     */
    private boolean isCompatible(Class<?>[] methodParams, Class<?>[] actualParams) {
        if (methodParams.length != actualParams.length) {
            return false;
        }

        for (int i = 0; i < methodParams.length; i++) {
            if (!methodParams[i].isAssignableFrom(actualParams[i])) {
                // 检查基本类型的包装类兼容性
                if (methodParams[i].isPrimitive() && isPrimitiveWrapper(methodParams[i], actualParams[i])) {
                    continue;
                }
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否为基本类型的包装类
     */
    private boolean isPrimitiveWrapper(Class<?> primitive, Class<?> wrapper) {
        if (primitive == int.class && wrapper == Integer.class)
            return true;
        if (primitive == long.class && wrapper == Long.class)
            return true;
        if (primitive == double.class && wrapper == Double.class)
            return true;
        if (primitive == float.class && wrapper == Float.class)
            return true;
        if (primitive == boolean.class && wrapper == Boolean.class)
            return true;
        if (primitive == char.class && wrapper == Character.class)
            return true;
        if (primitive == byte.class && wrapper == Byte.class)
            return true;
        if (primitive == short.class && wrapper == Short.class)
            return true;
        return false;
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    /**
     * 内存中的Java源文件
     */
    private static class JavaSourceObject extends SimpleJavaFileObject {
        private final String content;

        public JavaSourceObject(String name, String content) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

    /**
     * 处理响应式流对象（Flowable/Observable）
     */
    private void handleReactiveStream(Object streamResult, Consumer<String> consumer, URLClassLoader classLoader) {
        try {
            Class<?> streamClass = streamResult.getClass();

            // 尝试不同版本的RxJava Consumer接口
            Class<?> consumerClass = null;
            try {
                // 尝试RxJava 3
                consumerClass = Class.forName("io.reactivex.rxjava3.functions.Consumer", true, classLoader);
            } catch (ClassNotFoundException e1) {
                try {
                    // 尝试RxJava 2
                    consumerClass = Class.forName("io.reactivex.functions.Consumer", true, classLoader);
                } catch (ClassNotFoundException e2) {
                    consumer.accept("错误: 无法找到RxJava Consumer接口");
                    return;
                }
            }

            // 创建Consumer接口的实现
            Object consumerInstance = java.lang.reflect.Proxy.newProxyInstance(
                    classLoader,
                    new Class<?>[] { consumerClass },
                    (proxy, m, a) -> {
                        if (m.getName().equals("accept")) {
                            consumer.accept(a[0].toString());
                            return null;
                        }
                        return m.invoke(proxy, a);
                    });

            // 创建error handler consumer
            Object errorHandlerConsumer = java.lang.reflect.Proxy.newProxyInstance(
                    classLoader,
                    new Class<?>[] { consumerClass },
                    (proxy, m, a) -> {
                        if (m.getName().equals("accept") && a[0] instanceof Throwable) {
                            Throwable error = (Throwable) a[0];
                            consumer.accept("流式执行错误: " + error.getMessage());
                            return null;
                        }
                        return m.invoke(proxy, a);
                    });

            // 查找并调用subscribe方法（带错误处理）
            try {
                Method subscribeMethod = streamClass.getMethod("subscribe", consumerClass, consumerClass);
                subscribeMethod.invoke(streamResult, consumerInstance, errorHandlerConsumer);
            } catch (NoSuchMethodException e) {
                // 如果找不到带错误处理的subscribe方法，使用基本方法
                try {
                    Method subscribeMethod = streamClass.getMethod("subscribe", consumerClass);
                    subscribeMethod.invoke(streamResult, consumerInstance);
                } catch (NoSuchMethodException e2) {
                    consumer.accept("错误: 无法找到subscribe方法");
                    return;
                }
            }

            // 等待流处理完成（这里简单地通过等待一段时间实现，实际应用中可能需要更精确的方式）
            Thread.sleep(1000);

        } catch (Exception e) {
            consumer.accept("处理响应式流时发生异常: " + e.getMessage());
        }
    }
}
