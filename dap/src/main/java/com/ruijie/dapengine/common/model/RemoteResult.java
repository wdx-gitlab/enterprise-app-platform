package com.ruijie.dapengine.common.model;

import com.ruijie.dapengine.common.enums.ResultModelEnum;
import lombok.Data;

import java.io.Serializable;

@Data
@SuppressWarnings( "all" )
public class RemoteResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public RemoteResult() {
    }

    /**
     * 系统编码(部门标准200成功 500失败)
     */
    private String status;

    /**
     * 返回状态码描述信息
     */
    private String err;

    /**
     * 返回的数据信息
     */
    private T data;

    /**
     * 成功
     * @param data
     * @return
     */
    public static <T> RemoteResult<T> success( T data ){
        return builder(ResultModelEnum.SUCCESS.getCode() , ResultModelEnum.SUCCESS.getMessage() , data);
    }

    /**
     * 成功
     * @param <T>
     * @return
     */
    public static <T> RemoteResult<T> success(){
        return builder(ResultModelEnum.SUCCESS.getCode() , ResultModelEnum.SUCCESS.getMessage() , null);
    }

    /**
     * 成功
     * @param data
     * @return
     */
    public static <T> RemoteResult<T> success( String message ){
        return builder(ResultModelEnum.SUCCESS.getCode() , message , null);
    }

    /**
     * 失败
     * @param data
     * @return
     */
    public static <T> RemoteResult<T> failed( T data ){
        return builder(ResultModelEnum.FAILED.getCode() , ResultModelEnum.FAILED.getMessage() , data);
    }

    /**
     * 失败-消息
     * @param message
     * @return
     */
    public static <T> RemoteResult<T> failed( String message ){
        return builder(ResultModelEnum.FAILED.getCode() , message , null);
    }

    /**
     * 失败-消息-data
     * @param message
     * @param data
     * @param <T>
     * @return
     */
    public static <T> RemoteResult<T> failed( String message , T data ){
        return builder(ResultModelEnum.FAILED.getCode() , message , data);
    }

    /**
     * 异常-消息-data
     * @param message
     * @param <T>
     * @return
     */
    public static <T> RemoteResult<T> exception( String message ){
        return builder(ResultModelEnum.FAILED.getCode() , message , null );
    }

    /**
     * 私有构造构造器
     * @param code
     * @param isSuccess
     * @param message
     * @param data
     * @param <T>
     * @return
     */
    private static <T> RemoteResult<T> builder(String status , String err , T data){
        RemoteResult<T> result = new RemoteResult();
        result.setData(data);
        result.setErr(err);
        result.setStatus(status);
        return result;
    }

}
