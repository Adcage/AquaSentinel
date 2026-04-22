package com.springboot.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * EasyExcel 测试
*
 */
public class EasyExcelTest {

    @Test
    public void doImport() throws Exception {
        File file = Files.createTempFile("easyexcel-test", ".xlsx").toFile();
        EasyExcel.write(file)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet("sheet1")
                .doWrite(Collections.singletonList(Collections.singletonMap(0, "hello")));

        List<Map<Integer, String>> list = EasyExcel.read(file)
                .excelType(ExcelTypeEnum.XLSX)
                .sheet()
                .headRowNumber(0)
                .doReadSync();
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("hello", list.get(0).get(0));

        Files.deleteIfExists(file.toPath());
    }

}
