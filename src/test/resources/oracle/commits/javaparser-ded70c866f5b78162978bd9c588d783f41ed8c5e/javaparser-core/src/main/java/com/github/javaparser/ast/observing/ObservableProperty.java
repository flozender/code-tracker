package com.github.javaparser.ast.observing;

/**
 * Properties considered by the AstObserver
 */
public enum ObservableProperty {
    ANNOTATIONS,
    ANONYMOUS_CLASS_BODY,
    @Deprecated
    ARRAY_BRACKET_PAIRS_AFTER_ID,
    @Deprecated
    ARRAY_BRACKET_PAIRS_AFTER_TYPE,
    ARGUMENTS,
    BLOCK,
    BODY,
    CATCH_CLAUSES,
    CATCH_BLOCK,
    CHECK,
    CLASS_BODY,
    CLASS_EXPR,
    COMMENT,
    COMMENTED_NODE,
    COMPARE,
    COMPONENT_TYPE,
    CONDITION,
    CONTENT,
    DEFAULT_VALUE,
    DIMENSION,
    ELEMENTS,
    @Deprecated
    ELEMENT_TYPE,
    ELSE_EXPR,
    ELSE_STMT,
    ENTRIES,
    EXPRESSION,
    EXTENDED_TYPES,
    FIELD,
    FINALLY_BLOCK,
    IDENTIFIER,
    IMPLEMENTED_TYPES,
    IMPORTS,
    INDEX,
    INITIALIZER,
    INNER,
    IS_INTERFACE,
    ITERABLE,
    IS_THIS,
    LABEL,
    LEFT,
    LEVELS,
    MEMBERS,
    MEMBER_VALUE,
    MODIFIERS,
    MESSAGE,
    NAME,
    OPERATOR,
    PACKAGE_DECLARATION,
    PAIRS,
    PARAMETER,
    PARAMETERS,
    PARAMETERS_ENCLOSED,
    QUALIFIER,
    RANGE,
    RESOURCES,
    RIGHT,
    SCOPE,
    SELECTOR,
    STATIC,
    STATIC_MEMBER,
    STATEMENT,
    STATEMENTS,
    SUPER,
    TARGET,
    THEN_EXPR,
    THEN_STMT,
    THROWN_TYPES,
    TRY_BLOCK,
    TYPE,
    TYPES,
    TYPE_ARGUMENTS,
    TYPE_BOUND,
    TYPE_DECLARATION,
    TYPE_PARAMETERS,
    UPDATE,
    VALUE,
    VALUES,
    VARIABLE,
    VARIABLES,
    VAR_ARGS
}
