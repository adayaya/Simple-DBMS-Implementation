package org.example;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
////        String input = "create table tb_name (attr1 varchar(20) primary key,attr2 int)";
//        String input = "select attr1,attr2 from tb_name where id = 2";
////        List<String> parts = splitString(input);
////        String s = parts.get(4);
////        System.out.println(s);
////        String content = s.substring(1, s.length() - 1);
////        // 按逗号切分
////        String[] part = content.split(",");
////
////         //打印切分后的结果
////        for (String _part : part) {
////            System.out.println(_part);
////        }
//        String[] parts = input.split("\\s+");//按空格分开
//        String[] input1 = parts[1].split(",");
//        for(String part:input1){
//            System.out.println(part);
//        }
        String s="";
        int a=3;
        HashMap mp = new HashMap();
        switch ( s){

        }
    }

    private static List<String> splitString(String input) {
        List<String> parts = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        Stack<Character> stack = new Stack<>();

        for (char c : input.toCharArray()) {
            if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                stack.pop();
            }

            if (c == ' ' && stack.isEmpty()) {
                if (sb.length() > 0) {
                    parts.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }

        if (sb.length() > 0) {
            parts.add(sb.toString());
        }

        return parts;
    }
}