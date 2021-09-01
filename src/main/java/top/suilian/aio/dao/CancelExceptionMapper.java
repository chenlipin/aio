package top.suilian.aio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import top.suilian.aio.model.CancelException;

public interface CancelExceptionMapper {
    int deleteByPrimaryKey(Integer cancelExceptionId);

    int insert(CancelException record);

    CancelException selectByPrimaryKey(Integer cancelExceptionId);

    List<CancelException> selectAll();

    int updateByPrimaryKey(@Param("cancelException") CancelException cancelException);
}