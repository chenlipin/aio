package top.suilian.aio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.dao.ExceptionMessageMapper;
import top.suilian.aio.model.ExceptionMessage;

@Service("exceptionMessage")
public class ExceptionMessageService{
    @Autowired
    ExceptionMessageMapper exceptionMessageMapper;

    public int insert(ExceptionMessage exceptionMessage){
        return exceptionMessageMapper.insert(exceptionMessage);
    }
}
