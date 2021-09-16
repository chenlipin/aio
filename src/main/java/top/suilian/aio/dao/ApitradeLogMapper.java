package top.suilian.aio.dao;


import org.springframework.stereotype.Repository;
import top.suilian.aio.model.ApitradeLog;

@Repository
public interface ApitradeLogMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(ApitradeLog record);

    int insertSelective(ApitradeLog record);

    ApitradeLog selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(ApitradeLog record);

    int updateByPrimaryKey(ApitradeLog record);
}