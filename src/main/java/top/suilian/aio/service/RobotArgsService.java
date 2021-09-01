package top.suilian.aio.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.suilian.aio.dao.RobotArgsMapper;
import top.suilian.aio.model.RobotArgs;

import java.util.List;

@Service
public class RobotArgsService {
    @Autowired
    RobotArgsMapper robotArgsMapper;
    public RobotArgs findOne(Integer id, String variable){
        return robotArgsMapper.findOne(id, variable);
    }

    public int update(Integer id, String variable, String value){
        return robotArgsMapper.update(id, variable, value);
    }

    public List<RobotArgs> findAll(Integer id){
        return  robotArgsMapper.selectAll(id);
    }
}
