package top.suilian.aio.controller;

import lombok.Data;

import java.io.Serializable;

@Data
public class ApiReq implements Serializable  {
    private String  apiKey;
    private String  tpass;
    private String  exchange;
}
