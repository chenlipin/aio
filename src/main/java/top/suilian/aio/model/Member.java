package top.suilian.aio.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * member
 * @author 
 */
@Data
public class Member implements Serializable {
    /**
     * 用户表
     */
    private Integer memberId;

    private String username;

    private String password;

    private String name;

    private Boolean gender;

    private Date birthDate;

    private String mobile;

    private String email;

    /**
     * 用户头像文件名称
     */
    private String header;

    private String realname;

    private Integer activation;

    /**
     * 0:账号未激活
1:账号已激活
2:已冻结
     */
    private Integer status;

    private Boolean active;

    private Boolean deleted;

    private Date createdAt;

    private Date updatedAt;

    private static final long serialVersionUID = 1L;
}