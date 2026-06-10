package com.smartcampuslifeserver.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class CourseImportDTO {

    @ExcelProperty("课程名称")
    private String name;

    @ExcelProperty("上课周次")
    private String weekPattern;

    @ExcelProperty("星期")
    private String dayOfWeek;

    @ExcelProperty("起始节次")
    private String startPeriod;

    @ExcelProperty("结束节次")
    private String endPeriod;

    @ExcelProperty("上课地点")
    private String location;
}
