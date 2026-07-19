package com.github.jun8848.kubejsrecipeui.event;

/**
 * 事件方法参数在下拉框和代码模板中的描述。
 *
 * @param name 推断出的参数名称
 * @param type 简化后的 Java 类型名称
 * @param placeholder 生成方法调用时使用的默认占位值
 */
public record EventMethodParameter(String name, String type, String placeholder) {
}
