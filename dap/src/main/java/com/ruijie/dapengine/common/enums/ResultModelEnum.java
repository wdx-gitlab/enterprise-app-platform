package com.ruijie.dapengine.common.enums;

public enum ResultModelEnum {
    /**
     * 成功
     */
    SUCCESS( "200" , "成功" ),
    /**
     * 失败
     */
    FAILED( "500" , "失败" ),
    /**
     * SQL类异常信息
     */
    SQL_EXCEPTION("10001" , "SQL语句非法，请检测字段列" ),

    /**
     * 全局异常，当匹配不到合适的异常信息时执行该异常提醒
     */
    GLOBAL_EXCEPTION( "9999" , "发生异常,请联系管理员" );

    private String code;

    private String message;

    /**
     * 默认构造函数
     * @param code
     * @param message
     */
    ResultModelEnum(String code , String message){
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}