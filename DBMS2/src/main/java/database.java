
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class database {
    static int flag=0;
    static class table{
        //存放表的逻辑结构，存放着表的属性、主键、名称，物理表的地址（路径,还有维护索引的B plus tree
        private String tb_name; //
        private List<String>attr = new ArrayList<>();//存属性名
        private List<String> domain = new ArrayList<>(); //存域
        private String address;
        private String prim_key;
        private int cur_ptr = 1;//指向excel表中待写入的行号
        private BPlusTree<String,HashMap<String,String>> btree = new BPlusTree<>(6); //key:{属性：value}
        table(String _tb_name,List<String> attr_name,List<String> attr_domain,String _address,String _prim_key){
            // 表名，属性名，属性域，excel地址，主键名
            tb_name = _tb_name;
            attr.addAll(attr_name);
            domain.addAll(attr_domain);
            address = _address;
            prim_key = _prim_key;
        }
        table(){}
    }

     static class SQLEngine{
        private final String path = System.getProperty("user.dir");
        private String curDb;
        private HashMap<String,table> tableMap = new HashMap<>();
        SQLEngine(){
        }
        private void cDB(String db_name){
            //要完成创建文件夹，并指向当前数据库
            String DB_path = path+"/"+db_name;
            File folder = new File(DB_path);
            this.curDb = db_name;
            if(!folder.exists()){
                boolean created = folder.mkdir();
                if(created){
                    System.out.println("数据库创建成功\n");
                }
                else{
                    System.out.println("数据库创建失败\n");
                }
            }
        }
        private void cTb(String tb_name,List<String> _attr,List<String> domain){
            //要完成创建excel文件，写入属性，并建立逻辑表
            // [attr],[domain]
            table t = new table(tb_name,_attr,domain,path+"/"+this.curDb+"/"+tb_name+".xlsx",_attr.get(0));
            System.out.println("主键为： "+_attr.get(0));
            tableMap.put(this.curDb+tb_name,t);
            //创建excel文件
            Workbook workbook = new XSSFWorkbook(); // 创建新的Excel工作簿
            Sheet sheet = workbook.createSheet("Sheet1"); // 创建工作表
            Row row1 = sheet.createRow(0);
            for (int i = 0; i < _attr.size(); i++) {
                Cell cell = row1.createCell(i);
                cell.setCellValue(_attr.get(i));//写入属性
            }
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(path+"/"+this.curDb+"/"+tb_name+".xlsx");//path+
                workbook.write(fileOutputStream);
                fileOutputStream.close();
                System.out.println(tb_name+"表创建成功！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void insert(String tb_name,List<String> values){
            // insert into table_name values(value1,value2,...)
            // 要做的就是插入到物理表和b+树中
            table tb = tableMap.get(this.curDb+tb_name);
            String address = tb.address;
            if(tb.btree.query(values.get(0)).size()>0){
                System.out.println("主键重复，请重新输入");
                return;
            }
            HashMap<String,String> record = new HashMap<>(); //记录是一个字典，属性：属性值
            for (int i = 0; i < values.size(); i++) {
                record.put(tb.attr.get(i),values.get(i));
            }
            tb.btree.insert(values.get(0),record);//插入主键：记录
            try {
                // 加载现有的Excel文件
                FileInputStream fileInputStream = new FileInputStream(address);
                Workbook workbook = new XSSFWorkbook(fileInputStream);
                // 获取要操作的工作表
                Sheet sheet = workbook.getSheet("Sheet1");
                // 创建行和单元格，并填充数据
                Row row = sheet.createRow(tb.cur_ptr++);
                for (int i = 0; i < values.size(); i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(values.get(i)); //写入属性值
                }
                // 保存更改到现有的Excel文件
                FileOutputStream fileOutputStream = new FileOutputStream(address);
                workbook.write(fileOutputStream);
                // 关闭文件流
                fileOutputStream.close();
                fileInputStream.close();
//                System.out.println("数据写入成功！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private HashMap<String, List<String>> selectTb(String tb_name, List<String> selected, List<String> condition, int flag) throws IOException {
            //[attr1,attr2],[attr1 op value,..,],flag表示选的方式，只有在主键搜索或者主键范围搜索时使用到，flag为0表示按全表搜索，否则在b+树上搜索
            //要根据条件选择出相应的行，再根据selected，如果是*，那么就全选
            //返回一个map
            table tb = tableMap.get(this.curDb+tb_name);
            HashMap<String,List<String>> res = new HashMap<>(); //属性：属性值
            List<Integer> selected_num = new ArrayList<>();
            if(selected.get(0).equals("*")){
                for (int i = 0; i < tb.attr.size(); i++) {
                    selected_num.add(i);
                }
            }
            else{
                for (String s : selected) {
                    for (int j = 0; j < tb.attr.size(); j++) {
                        if (s.equals(tb.attr.get(j))) {
                            selected_num.add(j);
                            break;
                        }
                    }
                }
                //找到下标了
            }
            if(condition.size()==0){
                //没有条件
                try {
                    // 加载Excel文件
                    FileInputStream fileInputStream = new FileInputStream(tb.address);
                    Workbook workbook = new XSSFWorkbook(fileInputStream);
                    // 获取第一个工作表
                    Sheet sheet = workbook.getSheetAt(0);
                    int lastRowNum = sheet.getLastRowNum();
                    // 遍历列号的每一列
                    for (int colIndex : selected_num) {
                        List<String> tmp = new ArrayList<>(); //存一列数据
                        // 遍历每一行
                        for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                            Row row = sheet.getRow(rowIndex);
                            if (row != null) {
                                Cell cell = row.getCell(colIndex);
                                if (cell != null) {
                                    CellType cellType = cell.getCellType();
                                    String cellValue = "";
                                    if (cellType == CellType.STRING) {
                                        cellValue = cell.getStringCellValue();
                                    } else if (cellType == CellType.NUMERIC) {
                                        cellValue = String.valueOf(cell.getNumericCellValue());
                                    }
                                    tmp.add(cellValue);
                                }
                            }
                        }
                        res.put(tb.attr.get(colIndex),tmp); //属性：【属性值】
                    }
                    // 关闭文件流
                    fileInputStream.close();
                    return res;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(condition.size()==1){//value
                //如果是主键查询,只提供主键查询
                if(flag==1){//从B+树中寻找要选的属性
                    HashMap<String,String> rst= tb.btree.query(condition.get(0)).get(0);
                    for (String s:selected) {
                        List<String> tmp = new ArrayList<>();
                        tmp.add(rst.get(s));//值
                        res.put(s,tmp);//写到结果中
                    }
                }
                else{//如果是从excel中查询,那就需要遍历第一列中所有行，找到符合的行
                    // 加载Excel文件
                    FileInputStream fileInputStream = new FileInputStream(tb.address);
                    Workbook workbook = new XSSFWorkbook(fileInputStream);
                    // 获取第一个工作表
                    Sheet sheet = workbook.getSheetAt(0);
                    int lastRowNum = sheet.getLastRowNum();
                    // 遍历每一行
                    List<Integer> meetRow = new ArrayList<>();
                    for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row != null) {
                            Cell cell = row.getCell(0);//主键元素
                            if (cell != null) {
                                CellType cellType = cell.getCellType();
                                String cellValue = "";
                                if (cellType == CellType.STRING) {
                                    cellValue = cell.getStringCellValue();
                                } else if (cellType == CellType.NUMERIC) {
                                    cellValue = String.valueOf(cell.getNumericCellValue());
                                }
                                if(cellValue.equals(condition.get(0))){
                                    //记录行号
                                    meetRow.add(rowIndex);
                                }
                            }
                        }
                    }
                    // 遍历列号的每一列
                    for (int colIndex : selected_num) {
                        List<String> tmp = new ArrayList<>(); //存一列数据
                        // 遍历每一行
                        for (int rowIndex : meetRow) {
                            Row row = sheet.getRow(rowIndex);
                            if (row != null) {
                                Cell cell = row.getCell(colIndex);
                                if (cell != null) {
                                    CellType cellType = cell.getCellType();
                                    String cellValue = "";
                                    if (cellType == CellType.STRING) {
                                        cellValue = cell.getStringCellValue();
                                    } else if (cellType == CellType.NUMERIC) {
                                        cellValue = String.valueOf(cell.getNumericCellValue());
                                    }
                                    tmp.add(cellValue);
                                }
                            }
                        }
                        res.put(tb.attr.get(colIndex),tmp); //属性：【属性值】
                    }
                    // 关闭文件流
                    fileInputStream.close();
                }
                return res;
            }
            else{ //condition==2,范围搜索,[v1,v2]
               //如果是主键查询,只提供主键查询
                if (flag == 1) {//从B+树中寻找要选的属性
                    List<HashMap<String, String>> rstList = tb.btree.rangeQuery(condition.get(0),condition.get(1)); //记录列表
                    for (String s : selected) {
                        List<String> tmp = new ArrayList<>();
                        for (HashMap<String,String> hm:rstList){
                            tmp.add(hm.get(s));//添加筛选出来的所有记录的属性
                        }
                        res.put(s,tmp);
                    }
                }
                else{
                    // 加载Excel文件
                    FileInputStream fileInputStream = new FileInputStream(tb.address);
                    Workbook workbook = new XSSFWorkbook(fileInputStream);
                    // 获取第一个工作表
                    Sheet sheet = workbook.getSheetAt(0);
                    int lastRowNum = sheet.getLastRowNum();
                    // 遍历每一行
                    List<Integer> meetRow = new ArrayList<>();
                    for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row != null) {
                            Cell cell = row.getCell(0);//主键元素
                            if (cell != null) {
                                CellType cellType = cell.getCellType();
                                String cellValue = "";
                                if (cellType == CellType.STRING) {
                                    cellValue = cell.getStringCellValue();
                                } else if (cellType == CellType.NUMERIC) {
                                    cellValue = String.valueOf(cell.getNumericCellValue());
                                }
                                if(cellValue.compareTo(condition.get(0))>=0&&cellValue.compareTo(condition.get(1))<=0){
                                    //记录行号
                                    meetRow.add(rowIndex);
                                }
                            }
                        }
                    }
                    // 遍历列号的每一列
                    for (int colIndex : selected_num) {
                        List<String> tmp = new ArrayList<>(); //存一列数据
                        // 遍历每一行
                        for (int rowIndex : meetRow) {
                            Row row = sheet.getRow(rowIndex);
                            if (row != null) {
                                Cell cell = row.getCell(colIndex);
                                if (cell != null) {
                                    CellType cellType = cell.getCellType();
                                    String cellValue = "";
                                    if (cellType == CellType.STRING) {
                                        cellValue = cell.getStringCellValue();
                                    } else if (cellType == CellType.NUMERIC) {
                                        cellValue = String.valueOf(cell.getNumericCellValue());
                                    }
                                    tmp.add(cellValue);
                                }
                            }
                        }
                        res.put(tb.attr.get(colIndex),tmp); //属性：【属性值】
                    }
                    // 关闭文件流
                    fileInputStream.close();
                }
                return res;
            }
        return res;
        }
        private void update(String tb_name,HashMap<String,String> sets,List<String> condition) throws IOException {
            //表名，设置属性的字典，条件,更新B+树，更新excel,设置更新，对于满足某些条件的,sets，可以有很多更新的属性
            //update table tb_name set attr=newvalue where id = 1
            table tb = tableMap.get(this.curDb+tb_name);
            List<Integer> setsCol = new ArrayList<>();
            for(String setAttr:sets.keySet()){
                for (int i=0;i<tb.attr.size();i++){
                    if(setAttr.equals(tb.attr.get(i))){
                        setsCol.add(i);
                    }
                }
            }
            if(condition.size()==1){
                //更新B+树,B+树，是{primkey:{attr:attr_value}}
                HashMap<String,String> old = tb.btree.query(condition.get(0)).get(0);
                HashMap<String,String> new1 = new HashMap<>();
                for(String key:old.keySet()){
                    new1.put(key,old.get(key));
                }
                for(String setAttr:sets.keySet()){
                    String value = sets.get(setAttr);
                    new1.put(setAttr,value);
                }
                tb.btree.update(condition.get(0),old,new1);
                //更新excel
                // 加载Excel文件
                FileInputStream fileInputStream = new FileInputStream(tb.address);
                Workbook workbook = new XSSFWorkbook(fileInputStream);
                // 获取第一个工作表
                Sheet sheet = workbook.getSheetAt(0);
                int lastRowNum = sheet.getLastRowNum();
                // 遍历每一行,找到符合条件的记录
                for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null) {
                        Cell cell = row.getCell(0);
                        if (cell != null) {
                            CellType cellType = cell.getCellType();
                            String cellValue = "";
                            if (cellType == CellType.STRING) {
                                cellValue = cell.getStringCellValue();
                            } else if (cellType == CellType.NUMERIC) {
                                cellValue = String.valueOf(cell.getNumericCellValue());
                            }
                            if(cellValue.equals(condition.get(0))){
                                for (int col:setsCol){
                                    Cell newcell = row.getCell(col);
                                    newcell.setCellValue(sets.get(tb.attr.get(col)));//属性对应的新值
                                    try (FileOutputStream fos = new FileOutputStream(tb.address)) {
                                        workbook.write(fos);
                                    }
                                }
                            }
                        }
                    }
                }
                fileInputStream.close();
            }
            else if(condition.size()==2){
                //更新B+树，范围查找找到所有符合条件的记录{attr:value},然后对每条记录进行更新
                List<HashMap<String,String>> record = tb.btree.rangeQuery(condition.get(0), condition.get(1) );
                List<String> primKey = new ArrayList<>();
                for(HashMap<String,String> r:record){
                    primKey.add(r.get(tb.prim_key));
                }
                for(String pk:primKey){//对每个符合主键条件的记录
                    HashMap<String,String> old = tb.btree.query(pk).get(0);
                    HashMap<String,String> new1 = new HashMap<>();
                    for(String key:old.keySet()){
                        new1.put(key,old.get(key));
                    }
                    for(String setAttr:sets.keySet()){
                        String value = sets.get(setAttr);
                        new1.put(setAttr,value);
                    }
                    tb.btree.update(pk,old,new1);
                }
                //更新excel
                // 加载Excel文件
                FileInputStream fileInputStream = new FileInputStream(tb.address);
                Workbook workbook = new XSSFWorkbook(fileInputStream);
                // 获取第一个工作表
                Sheet sheet = workbook.getSheetAt(0);
                int lastRowNum = sheet.getLastRowNum();
                // 遍历每一行,找到符合条件的记录
                for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null) {
                        Cell cell = row.getCell(0); //主键属性值
                        if (cell != null) {
                            CellType cellType = cell.getCellType();
                            String cellValue = "";
                            if (cellType == CellType.STRING) {
                                cellValue = cell.getStringCellValue();
                            } else if (cellType == CellType.NUMERIC) {
                                cellValue = String.valueOf(cell.getNumericCellValue());
                            }
                            if(cellValue.compareTo(condition.get(0))>=0&&cellValue.compareTo(condition.get(1))<=0){
                                for (int col:setsCol){
                                    Cell newcell = row.getCell(col);
                                    newcell.setCellValue(sets.get(tb.attr.get(col)));//属性对应的新值
                                    try (FileOutputStream fos = new FileOutputStream(tb.address)) {
                                        workbook.write(fos);
                                    }
                                }
                            }
                        }
                    }
                }
                fileInputStream.close();
            }
        }
        private void delete(String tb_name,List<String> condition) throws IOException {
            //删除B+树的记录，删除excel表内容
            table tb = tableMap.get(this.curDb+tb_name);
            //根据condition找到符合条件的记录
            if(condition.size()==1){
                //删除B+树记录
                tb.btree.remove(condition.get(0));
                //删除excel记录，要找到是哪一行
                int rowDelete = -1;
                // 加载Excel文件
                FileInputStream fileInputStream = new FileInputStream(tb.address);
                Workbook workbook = new XSSFWorkbook(fileInputStream);
                // 获取第一个工作表
                Sheet sheet = workbook.getSheetAt(0);
                int lastRowNum = sheet.getLastRowNum();
                // 遍历每一行,找到符合条件的记录
                for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null) {
                        Cell cell = row.getCell(0); //主键属性值
                        if (cell != null) {
                            CellType cellType = cell.getCellType();
                            String cellValue = "";
                            if (cellType == CellType.STRING) {
                                cellValue = cell.getStringCellValue();
                            } else if (cellType == CellType.NUMERIC) {
                                cellValue = String.valueOf(cell.getNumericCellValue());
                            }
                            if(cellValue.compareTo(condition.get(0))==0){
                                rowDelete = rowIndex;
                                break;
                            }
                        }
                    }
                }
                fileInputStream.close();
                String filePath = tb.address;
                String sheetName = "Sheet1";
                 // 要删除的行索引（从0开始）
                try (FileInputStream fis = new FileInputStream(filePath);
                     Workbook workbook1 = new XSSFWorkbook(fis)) {
                    Sheet sheet1 = workbook1.getSheet(sheetName);
                    if (sheet1.getRow(rowDelete) != null) {
                        Row row = sheet1.getRow(rowDelete);
                        sheet1.removeRow(row);
                        sheet1.shiftRows(rowDelete + 1, sheet1.getLastRowNum(), -1);
                    }
                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        workbook1.write(fos);
                    }
                    tb.cur_ptr -= 1;
                    System.out.println("记录删除成功！");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                //删除B+树记录，找出所有符合条件的主键
                List<HashMap<String,String>> record = tb.btree.rangeQuery(condition.get(0), condition.get(1) );
                List<String> primKey = new ArrayList<>();
                for(HashMap<String,String> r:record){
                    primKey.add(r.get(tb.prim_key));
                }
                for(String pk:primKey){
                    tb.btree.remove(pk);
                }
                //删除excel记录，要每次都遍历一遍，直到删除完毕
                while(true){
                    //删除excel记录，要找到是哪一行
                    int rowDelete = -1;
                    // 加载Excel文件
                    FileInputStream fileInputStream = new FileInputStream(tb.address);
                    Workbook workbook = new XSSFWorkbook(fileInputStream);
                    // 获取第一个工作表
                    Sheet sheet = workbook.getSheetAt(0);
                    int lastRowNum = sheet.getLastRowNum();
                    // 遍历每一行,找到符合条件的记录
                    for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row != null) {
                            Cell cell = row.getCell(0); //主键属性值
                            if (cell != null) {
                                CellType cellType = cell.getCellType();
                                String cellValue = "";
                                if (cellType == CellType.STRING) {
                                    cellValue = cell.getStringCellValue();
                                } else if (cellType == CellType.NUMERIC) {
                                    cellValue = String.valueOf(cell.getNumericCellValue());
                                }
                                if(cellValue.compareTo(condition.get(0))>=0&&cellValue.compareTo(condition.get(1))<=0){
                                    rowDelete = rowIndex;
                                    break;
                                }
                            }
                        }
                    }
                    if(rowDelete==-1){
                        break;
                    }
                    fileInputStream.close();
                    String filePath = tb.address;
                    String sheetName = "Sheet1";
                    // 要删除的行索引（从0开始）
                    try (FileInputStream fis = new FileInputStream(filePath);
                         Workbook workbook1 = new XSSFWorkbook(fis)) {
                        Sheet sheet1 = workbook.getSheet(sheetName);
                        if (sheet1.getRow(rowDelete) != null) {
                            Row row = sheet1.getRow(rowDelete);
                            sheet1.removeRow(row);
                            sheet1.shiftRows(rowDelete + 1, sheet1.getLastRowNum(), -1);
                        }
                        try (FileOutputStream fos = new FileOutputStream(filePath)) {
                            workbook1.write(fos);
                        }
                        tb.cur_ptr -= 1;
                        System.out.println("记录删除成功！");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        private void useDB(String DB_name){
            this.curDb = DB_name;
        }
        private void dropTb(String tb_name){
            //删除逻辑表和物理表
            table tb = tableMap.get(this.curDb+tb_name);
            File file = new File(tb.address);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println(tb_name+"表删除成功.");
                } else {
                    System.out.println("无法删除");
                }
            } else {
                System.out.println(tb_name+"表不存在");
            }
            tableMap.remove(this.curDb+tb_name);
        }
        private List<String> splitString(String input) {
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
        private void show(HashMap<String,List<String>> res){
//            List<List<String>> rst = new ArrayList<>();
//            //id： 123，213->
//            List<String> attr = new ArrayList<>(res.keySet());
//            Collections.reverse(attr);
//            rst.add(attr);
//            for(int i=0;i<rst.get(0).size();i++){
//                List<String> tmp = new ArrayList<>();
//                for (String s : attr) {
//                    tmp.add(res.get(s).get(i));
//                }
//                rst.add(tmp);
//            }
//            for (List<String> strings : rst) {
//                for (int j = 0; j < rst.get(0).size(); j++) {
//                    System.out.print(strings.get(j) + "\t");
//                }
//                System.out.println();
//            }
            for(String key:res.keySet()){
                System.out.print(key+":\t");
                for(String value:res.get(key)){
                    System.out.print(value+"\t");
                }
                System.out.println();
            }
        }
        public void SQLParse(String sql) throws IOException {
            //对sql进行解析
            //首先是对sql进行小写转换
            String lowerSQL = sql.toLowerCase();
            // 1.create table/database tb_name (attr1 varchar(20) primary key,attr2 int)
            // 2.insert into ** values()
            // 3.select ** from table where a op b and a op c
            // 4.update ** set ** where **
            // 5.delete from ** where**
            // 6.drop table
            // 7.use db_name
            String[] parts = lowerSQL.split("\\s+");
            switch (parts[0]) {
                case "create":
                    //Create table student values (id varchar(10) primary key,name varchar(10))
                    if (parts[1].equals("table")) {
                        List<String> part = splitString(lowerSQL);
                        String tb_name = part.get(2);
                        String s = part.get(4);
                        String content = s.substring(1, s.length() - 1);
                        String[] part2 = content.split(",");
                        //(String tb_name,List<String> _attr,List<String> domain)
                        List<String> attr = new ArrayList<>();
                        List<String> domain = new ArrayList<>();
                        for (String _part : part2) {
                            String[] part3 = _part.trim().split("\\s+");
                            attr.add(part3[0]);
                            domain.add(part3[1]);
                        }
                        cTb(tb_name, attr, domain);//调用函数

                    } else if (parts[1].equals("database")) {
                        //(String db_name)
                        String db_name = parts[2];
                        cDB(db_name);//调用函数
                    } else {
                        System.out.println("wrong sql command!");
                    }
                    break;
                case "insert":
                    //(String tb_name,List<String> values)
                    //"insert into tb_name values (value1,value2)"
                    List<String> part = splitString(lowerSQL);
                    String s = part.get(4); //(value1,value2)
                    String content = s.substring(1, s.length() - 1);
                    // 按逗号切分
                    String tb_name = part.get(2);
                    String[] part1 = content.split(",");
                    List<String> values = new ArrayList<>();
                    Collections.addAll(values, part1);
                    insert(tb_name, values);//调用函数
                    break;
                case "select":
                    //(String tb_name, List<String> selected, List<String> condition, int flag)
                    //select */attr1,attr2 from tb_name where id = 2/id >= 4&&id <= 5
                    String[] input = lowerSQL.split("\\s+");
                    String tb_name_s = input[3];
                    List<String> selected = new ArrayList<>();
                    List<String> condition = new ArrayList<>();
                    if(input[1].equals("*")){
                        selected.add("*");
                    }else {
                        String[] input1 = input[1].split(",");
                        for(String s1:input1){
                            selected.add(s1);
                        }
                    }
                    if(input.length==8){
                        condition.add(input[7]);
                    }
                    else if(input.length==12){
                        condition.add(input[7]);
                        condition.add(input[11]);
                    }
                    Long start = System.currentTimeMillis();
                    HashMap<String,List<String>> rst = selectTb(tb_name_s,selected,condition,flag);//调用函数
                    Long end = System.currentTimeMillis();
                    if (flag==1){
                        System.out.println("使用B+树索引，共花时： "+(end-start)+"ms");
                    }
                    else{
                        System.out.println("不使用B+树索引，共花时： "+(end-start)+"ms");
                    }
                    show(rst);//打印结果
                    break;
                case "update":
                    //(String tb_name,HashMap<String,String> sets,List<String> condition)
                    //update ** set attr1=value1,attr2=value2 where id = 3/id >=3 && id<=5
                    String[] input_u = lowerSQL.split("\\s+");
                    String tb_name_u = input_u[1];
                    HashMap<String,String> sets = new HashMap<>();
                    List<String> condition_u = new ArrayList<>();
                    String[] input_u_attr = input_u[3].split(",");
                    for(String s_u:input_u_attr){
                        String[] s_u1 = s_u.split("=");
                        sets.put(s_u1[0],s_u1[1]);
                    }
                    if(input_u.length==8){
                        condition_u.add(input_u[7]);
                    }
                    else if(input_u.length==12){
                        condition_u.add(input_u[7]);
                        condition_u.add(input_u[11]);
                    }
//                    for (String c:condition_u){
//                        System.out.println(c);
//                    }
                    update(tb_name_u,sets,condition_u);
                    break;
                case "delete":
                    //(String tb_name,List<String> condition)
                    //delete from tb_name where id = 3/ id >=3 and id <= 5
                    String[] input_d = lowerSQL.split("\\s+");
                    String tb_name_d = input_d[2];
                    List<String> condition_d = new ArrayList<>();
                    if(input_d.length==7){
                        condition_d.add(input_d[6]);
                    }
                    else if(input_d.length==11){
                        condition_d.add(input_d[6]);
                        condition_d.add(input_d[10]);
                    }
                    delete(tb_name_d,condition_d);//调用函数
                    break;
                case "drop":
                    //(String tb_name)
                    //drop table db_name
                    String[] input_drop = lowerSQL.split("\\s+");
                    String tb_name_drop = input_drop[2];
                    dropTb(tb_name_drop);//调用函数
                    break;
                case "use":
                    //(String DB_name)
                    //use database db_name
                    String [] input_use = lowerSQL.split("\\s+");
                    String db_name = input_use[2];
                    useDB(db_name);//调用函数
                    System.out.println("当前指向数据库为： "+this.curDb);
                    break;
                default:
                    System.out.println("wrong sql command!");
                    break;
            }
        }
    }
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        SQLEngine sqlEngine = new SQLEngine();
        System.out.println("欢迎进入Group14的DBMS系统,请输入sql命令，按q退出！");
        System.out.println("如果想用B+树索引，请输入命令: cBtree;\n否则输入命令:nBtree(默认)");
        List<String> sqlList = new ArrayList<>();
        sqlList.add("create database db1");
        sqlList.add("Use database db1");
        sqlList.add("Create table student values (id varchar(10) primary key,name varchar(10))");
        for(int i=0;i<2000;i++){
            sqlList.add("insert into student values ("+i+","+i+1+")");
        }
        for(String s:sqlList){
            sqlEngine.SQLParse(s);
        }
        System.out.println("预指令全部加载完成！请输入命令");
        String sql = scanner.nextLine();
        while (!sql.equals("q")){
            if(sql.equals("cBtree")){
                flag = 1;
                System.out.println("切换为B+树索引");
            } else if (sql.equals("nBtree")) {
                flag = 0;
                System.out.println("切换为无索引");
            } else{
                sqlEngine.SQLParse(sql);
            }
            sql = scanner.nextLine();
        }
    }
}
