package com.mt.agent.ai.exception;

import com.mt.agent.ai.enums.AliModelType;
import com.mt.agent.ai.enums.ModelCapability;
import com.mt.agent.ai.enums.SiliconFlowModelType;

/**
 * 模型能力不支持异常
 */
public class CapabilityNotSupportedException extends ModelException {

    public CapabilityNotSupportedException(String modelCode, String capability) {
        super("模型 " + modelCode + " 不支持能力: " + capability);
    }

    public CapabilityNotSupportedException(ModelCapability capability, String message) {
        super("能力不支持: " + capability.getDescription() + "。" + message);
    }

    public CapabilityNotSupportedException(AliModelType aliModelType, ModelCapability capability) {
        super("模型 " + aliModelType.getName() + " (" + aliModelType.getCode() + ") 不支持能力: " + capability.getDescription());
    }

    public CapabilityNotSupportedException(AliModelType aliModelType, ModelCapability capability, String customMessage) {
        super("模型 " + aliModelType.getName() + " (" + aliModelType.getCode() + ") 不支持能力: " +
                capability.getDescription() + "。" + customMessage);
    }

    public CapabilityNotSupportedException(AliModelType aliModelType, ModelCapability capability, String recommendedModel,
                                           String message) {
        super("模型 " + aliModelType.getName() + " (" + aliModelType.getCode() + ") 不支持能力: " +
                capability.getDescription() + "，建议使用: " + recommendedModel + "。" + message);
    }

    public CapabilityNotSupportedException(SiliconFlowModelType modelType, ModelCapability capability) {
        super("硅基流动模型 " + modelType.getName() + " (" + modelType.getCode() + ") 不支持能力: " + capability.getDescription());
    }

    public CapabilityNotSupportedException(SiliconFlowModelType modelType, ModelCapability capability,
            String customMessage) {
        super("硅基流动模型 " + modelType.getName() + " (" + modelType.getCode() + ") 不支持能力: " +
                capability.getDescription() + "。" + customMessage);
    }
}