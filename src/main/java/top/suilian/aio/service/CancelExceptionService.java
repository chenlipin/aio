package top.suilian.aio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.dao.CancelExceptionMapper;
import top.suilian.aio.model.CancelException;

@Service("cancelException")
public class CancelExceptionService {
    @Autowired
    CancelExceptionMapper cancelExceptionMapper;

    public int insert(CancelException cancelException){
        return cancelExceptionMapper.insert(cancelException);
    }
}
