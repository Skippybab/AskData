# -*- coding: utf-8 -*-
"""
Generic Database Executor Helper Module

This script provides functions to execute SQL queries and return results as JSON.
It reads database configuration from a properties file.
"""

import configparser
import json
import sys
from pathlib import Path

def get_db_connection(db_type, host, port, db_name, user, password):
    """Establishes a database connection using the appropriate driver."""
    if db_type.lower() == 'mysql':
        try:
            import pymysql
            import pymysql.cursors
        except ImportError:
            raise ImportError("pymysql is not installed. Please install it via 'pip install pymysql'.")
        return pymysql.connect(
            host=host,
            port=int(port),
            user=user,
            password=password,
            database=db_name,
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
    elif db_type.lower() == 'postgresql':
        try:
            import psycopg2
            import psycopg2.extras
        except ImportError:
            raise ImportError("psycopg2 is not installed. Please install it via 'pip install psycopg2-binary'.")
        return psycopg2.connect(
            host=host,
            port=port,
            dbname=db_name,
            user=user,
            password=password,
            cursor_factory=psycopg2.extras.DictCursor
        )
    else:
        raise ValueError(f"Unsupported database type: {db_type}")

def json_default_converter(o):
    """Converts special types to string for JSON serialization."""
    import datetime
    import decimal
    if isinstance(o, (datetime.datetime, datetime.date)):
        return o.isoformat()
    if isinstance(o, decimal.Decimal):
        return str(o)
    raise TypeError(f"Object of type {o.__class__.__name__} is not JSON serializable")

class SecurityException(Exception):
    pass

def gen_sql(question, table_name):
    """
    Generate SQL query based on the question and table name.
    This is a simple implementation that creates basic SELECT queries.
    For more complex queries, this should be enhanced with AI/LLM integration.
    
    Args:
        question (str): The user's question
        table_name (str): The table name to query
        
    Returns:
        str: Generated SQL query
    """
    # Simple SQL generation logic
    # In a real implementation, this would call an AI service to generate SQL
    if "count" in question.lower() or "数量" in question or "总数" in question:
        return f"SELECT COUNT(*) as count FROM {table_name}"
    elif "all" in question.lower() or "全部" in question or "所有" in question:
        return f"SELECT * FROM {table_name} LIMIT 100"
    else:
        return f"SELECT * FROM {table_name} LIMIT 50"

def execute_query_and_get_json(sql_query):
    """The main function called by user's code."""
    if not sql_query or not isinstance(sql_query, str):
        raise ValueError("sql_query must be a non-empty string.")

    # Security check: only allow SELECT statements
    if not sql_query.strip().lower().startswith('select'):
        raise SecurityException("Only SELECT queries are allowed.")

    config_path = Path(__file__).parent / 'db_config.properties'
    if not config_path.exists():
        raise FileNotFoundError(f"db_config.properties not found at {config_path}")

    try:
        config = configparser.ConfigParser()
        config.read(config_path, encoding='utf-8')
        
        # 检查是否有配置节
        if not config.sections() and 'DEFAULT' not in config:
            raise ValueError("No configuration sections found in db_config.properties")
        
        # 优先使用DEFAULT节，如果没有则使用第一个节
        if 'DEFAULT' in config:
            db_config = config['DEFAULT']
        else:
            db_config = config[config.sections()[0]]
        
        # 验证必需的配置项
        required_keys = ['DB_TYPE', 'DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD']
        missing_keys = [key for key in required_keys if not db_config.get(key)]
        if missing_keys:
            raise ValueError(f"Missing required configuration keys: {missing_keys}")
        
        conn = get_db_connection(
            db_type=db_config.get('DB_TYPE'),
            host=db_config.get('DB_HOST'),
            port=db_config.get('DB_PORT'),
            db_name=db_config.get('DB_NAME'),
            user=db_config.get('DB_USER'),
            password=db_config.get('DB_PASSWORD')
        )

        try:
            with conn.cursor() as cursor:
                cursor.execute(sql_query)
                result = cursor.fetchall()
            return json.dumps(result, default=json_default_converter, ensure_ascii=False)
        finally:
            if conn:
                conn.close()
    except configparser.Error as e:
        raise ValueError(f"Error parsing db_config.properties: {e}")
    except Exception as e:
        raise RuntimeError(f"Database connection or query execution failed: {e}")