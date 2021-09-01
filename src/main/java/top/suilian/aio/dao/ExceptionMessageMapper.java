package top.suilian.aio.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import top.suilian.aio.model.ExceptionMessage;

public interface ExceptionMessageMapper {
    int deleteByPrimaryKey(Integer exceptionMessageId);

    int insert(@Param("exceptionMessage") ExceptionMessage exceptionMessage);

    ExceptionMessage selectByPrimaryKey(Integer exceptionMessageId);

    List<ExceptionMessage> selectAll();

    int updateByPrimaryKey(ExceptionMessage record);
}