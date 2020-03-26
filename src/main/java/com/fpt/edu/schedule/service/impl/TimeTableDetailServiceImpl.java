package com.fpt.edu.schedule.service.impl;

import com.fpt.edu.schedule.model.TimetableDetail;
import com.fpt.edu.schedule.repository.base.BaseSpecifications;
import com.fpt.edu.schedule.repository.base.LecturerRepository;
import com.fpt.edu.schedule.repository.base.QueryParam;
import com.fpt.edu.schedule.repository.base.TimetableRepository;
import com.fpt.edu.schedule.service.base.TimeTableDetailService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class TimeTableDetailServiceImpl implements TimeTableDetailService {
    LecturerRepository lecturerRepository;
    TimetableRepository timetableRepository;

    @Override
    public void addTimeTableDetail(TimetableDetail timetableDetail) {

    }

    @Override
    public List<TimetableDetail> getAllSchedule() {
        return timetableRepository.findAll();
    }

    @Override
    public List<TimetableDetail> findByCriteria(QueryParam queryParam) {
        BaseSpecifications cns = new BaseSpecifications(queryParam);

        return timetableRepository.findAll(cns);
    }


}
