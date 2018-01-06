import java.util.regex.*;
import java.util.*;
import java.io.*;

public class Compiler{
	public static final String INCLUDE = "include \\masm32\\include\\masm32rt.inc\r\ninclude drd.inc\r\nincludelib drd.lib\r\n\r\n";
	static int currentBoolean = 0;
	public static void writeToFile(String code,String dest){
		try {
			File destfile = new File(dest);
			if(!destfile.exists())
				destfile.createNewFile();
			FileWriter fwriter = new FileWriter(destfile);
			BufferedWriter bwriter = new BufferedWriter(fwriter);
			bwriter.write(code);
			bwriter.close();
			fwriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static int currentCondition = 0;
	static int currentRepeatLoop = 0;
	public static <T> void sop(T v) {
        System.out.println(v);
    }
    public String getSizeComponent(int size) {
    	return size==4?"DWORD":size==2?"WORD":"BYTE";
    }
	public static Scanner reader = new Scanner(System.in);
	static Datatable<String,Datatable<String,String>> functionsArguments = new Datatable<String,Datatable<String,String>>();
	static Datatable<String,Datatable<String,String>> classFields = new Datatable<String,Datatable<String,String>>();
	static Datatable<String,Integer> classSize = new Datatable<String,Integer>();
	static Stack<String> whileLoopBoolean = new Stack<String>();
	static Stack<String> repeatLoopNames = new Stack<String>();
	static {
		classSize.set("int", 4);
		classSize.set("string", 4);
		classSize.set("bool", 1);
		classSize.set("byte", 1);
		classSize.set("Image", 20);
		classSize.set("Point", 8);
		classSize.set("hex", 4);
		classSize.set("short", 2);
		classFields.set("Point",new Datatable<String,String>());
		classFields.get("Point").set("x", "int");
		classFields.get("Point").set("y", "int");
		classFields.set("Image", new Datatable<String,String>());
		classFields.get("Image").set("surface", "int");
		classFields.get("Image").set("iwidth", "int");
		classFields.get("Image").set("iheight", "int");
		classFields.get("Image").set("hasSrcKey", "int");
		classFields.get("Image").set("hbitmap", "int");
		functionsArguments.set("drd_init",new Datatable<String,String>());
		functionsArguments.get("drd_init").set("wdth", "int");
		functionsArguments.get("drd_init").set("heght", "int");
		functionsArguments.get("drd_init").set("flags", "int");
		functionsArguments.set("drd_pixelsClear",new Datatable<String,String>());
		functionsArguments.get("drd_pixelsClear").set("color", "hex");
		functionsArguments.set("drd_flip",new Datatable<String,String>());
		functionsArguments.set("drd_setKeyHandler",new Datatable<String,String>());
		functionsArguments.get("drd_setKeyHandler").set("func", "function");
		functionsArguments.set("drd_setMouseHandler",new Datatable<String,String>());
		functionsArguments.get("drd_setMouseHandler").set("func", "function");
		functionsArguments.set("drd_processMessages",new Datatable<String,String>());
		functionsArguments.set("drd_imageLoadFile",new Datatable<String,String>());
		functionsArguments.get("drd_imageLoadFile").set("filename", "string");
		functionsArguments.get("drd_imageLoadFile").set("pimg", "Image");
		functionsArguments.set("drd_imageDraw",new Datatable<String,String>());
		functionsArguments.get("drd_imageDraw").set("pimg", "Image");
		functionsArguments.get("drd_imageDraw").set("dstx", "int");
		functionsArguments.get("drd_imageDraw").set("dsty", "int");
		functionsArguments.set("drd_imageDrawCrop",new Datatable<String,String>());
		functionsArguments.get("drd_imageDrawCrop").set("pimg", "Image");
		functionsArguments.get("drd_imageDrawCrop").set("dstx", "int");
		functionsArguments.get("drd_imageDrawCrop").set("dsty", "int");
		functionsArguments.get("drd_imageDrawCrop").set("srcx", "int");
		functionsArguments.get("drd_imageDrawCrop").set("srcy", "int");
		functionsArguments.get("drd_imageDrawCrop").set("srcwidth", "int");
		functionsArguments.get("drd_imageDrawCrop").set("srcheight", "int");
		functionsArguments.set("drd_imageSetTransparent",new Datatable<String,String>());
		functionsArguments.get("drd_imageSetTransparent").set("pimg", "Image");
		functionsArguments.get("drd_imageSetTransparent").set("color", "hex");
		functionsArguments.set("drd_imageDelete",new Datatable<String,String>());
		functionsArguments.get("drd_imageDelete").set("pimg", "Image");
		functionsArguments.set("drd_setWindowTitle",new Datatable<String,String>());
		functionsArguments.get("drd_setWindowTitle").set("strng", "string");
		functionsArguments.set("sleep",new Datatable<String,String>());
		functionsArguments.get("sleep").set("time", "int");
	}
	static Stack<String> whileLoopNames = new Stack<String>();
	public static int getIndex(String cls,String var) {
		int index = 0;
		Object[] en = classFields.get(cls).keys();
		for(Object elem:en) {
			if(((String)(elem)).equals(var)) {
				return index;
			}
			index += classSize.get(classFields.get(cls).get((String)elem));
		}
		throw new IllegalArgumentException("you tried to access a not existing class member");
	}
	public static boolean isPrimative(String s) {
		return s.matches("(int)|(byte)|(bool)|(short)|(hex)");
	}
	static int currentTag = 0;
	static String currentFunction = null;
    static Stack<Integer> tagStack = new Stack<Integer>();
    
    static Stack<Integer> conditionStack = new Stack<Integer>();
    public static String compile(String path){
        String code = ".code\r\n";
        String data = ".data\r\n";
        String include = "\r\n";
        String codeFile;
        try{
            codeFile = new Scanner(new File(path)).useDelimiter("\\A").next(); // read the whole file
        }
        catch(IOException e){return null;}
        /*int flag1 = codeFile.indexOf("\""),flag2 = codeFile.substring(flag1+1).indexOf("\"")+flag1; // reserve all spaces in string
        while(flag1!=-1&&flag2!=-1) {
            String temp = codeFile.substring(flag1, flag2+2);
            String rep = temp.replaceAll(";","-&psikuda&~").
                        replaceAll("\\s","-&psikuda&-");
            codeFile = codeFile.replace(temp, rep);
            flag1 = codeFile.indexOf("\"");
            flag2 = codeFile.substring(flag1+1).indexOf("\"")+flag1;
        }*/
        codeFile = codeFile.replaceAll("\\s","").replaceAll(";","\r\n");
        String[] linesOfCode = codeFile.split("\\r\\n");
        Pattern pattern;
        Matcher matcher;
        String currentClass = null;
        int currentClassSize = 0;
        for(String line:linesOfCode) {
        	if(line.matches("include(lib)?.*")) {
        		if(line.matches("includelib.*"))
        			include += line.replaceFirst("includelib", "includelib ")+"\r\n";
        		else
        			include += line.replaceFirst("include", "include ")+"\r\n";
        	}
        	else if(line.matches("((Image)|(string)|(Point)|(int)|(byte)|(bool)|(short)|(hex)|([A-Z]+))\\[\\d+\\].*")) {
        		pattern = Pattern.compile("((Image)|(string)|(Point)|(byte)|(int)|(bool)|(short)|(hex)|([A-Z]+))");
        		matcher = pattern.matcher(line);
        		matcher.find();
        		String type = matcher.group(0);
        		line = line.replaceFirst(type, "");
        		String size = line.substring(1, line.indexOf(']'));
        		line = line.substring(size.length()+2);
        		if(type.equals("int")) {
        			data += line+" DWORD "+size+" times dup(?)\r\n";
        		}
        		else if(type.equals("byte")) {
        			data += line+" BYTE "+size+" times dup(?)\r\n";
        		}
        		else if(type.equals("short")) {
        			data += line+" WORD "+size+" times dup(?)\r\n";
        		}
        		else if(type.matches("Image")) {
        			data += line+" Img "+size+" times dup(<>)\r\n";
        		}
        		else if(type.matches("Point")) {
        			data+=line+" POINT "+size+" times dup(<>)\r\n";
        		}
        		else if(type.equals("bool")) {
        			data += line+" BYTE "+size+" times dup(?)\r\n";
        		}
        		else if(type.matches("hex")) {
        			data += line+" DWORD "+size+" times dup(?)\r\n";
        		}
        		else if(type.matches("[a-zA-Z_]\\w*")) {
        			data+=line+" "+type+" "+size+" times dup(<>)\r\n";
        		}
        	}
        	else if(line.matches("((Image)|(string)|(Point)|(int)|(byte)|(bool)|(short)|(hex)|([A-Z]+)).*")) {
        		if(currentClass!=null)
        			data+="\t";
        		pattern = Pattern.compile("((Image)|(string)|(Point)|(byte)|(int)|(bool)|(short)|(hex)|([A-Z]+))");
        		matcher = pattern.matcher(line);
        		matcher.find();
        		String type = matcher.group(0);
        		line = line.replaceFirst(type, "");
        		if(currentClass!=null) {
        			classFields.get(currentClass).set(line, type);
        			currentClassSize += classSize.get(type);
        		}
        		if(type.equals("int")) {
        			if(line.matches("[a-zA-Z_]\\w*")) {
        				data += line+" DWORD ?\r\n";
        			}
        			else {
        				pattern = Pattern.compile("=-?\\d+");
        				matcher = pattern.matcher(line);
        				matcher.find();
        				line = line.replaceAll(matcher.group(0), "");
        				data += line + " DWORD "+matcher.group(0).substring(1)+"\r\n";
        			}
        		}
        		else if(type.equals("byte")) {
        			if(line.matches("[a-zA-Z_]\\w*")) {
        				data += line+" BYTE ?\r\n";
        			}
        			else {
        				pattern = Pattern.compile("=-?\\d+");
        				matcher = pattern.matcher(line);
        				matcher.find();
        				line = line.replaceAll(matcher.group(0), "");
        				data += line + " BYTE "+matcher.group(0).substring(1)+"\r\n";
        			}
        		}
        		else if(type.equals("short")) {
        			if(line.matches("[a-zA-Z_]\\w*")) {
        				data += line+" WORD ?\r\n";
        			}
        			else {
        				pattern = Pattern.compile("=-?\\d+");
        				matcher = pattern.matcher(line);
        				matcher.find();
        				line = line.replaceAll(matcher.group(0), "");
        				data += line + " WORD "+matcher.group(0).substring(1)+"\r\n";
        			}
        		}
        		else if(type.matches("Image")) {
        			data += line+" Img<>\r\n";
        		}
        		else if(type.matches("Point")) {
        			pattern = Pattern.compile("\\(.*\\)");
        			matcher = pattern.matcher(line);
        			if(matcher.find()) {
        				data+=line.replaceAll(matcher.group(0), "").replaceAll("\\(|\\)","") + " POINT<"+matcher.group(0).substring(1,matcher.group(0).length()-1)+">\r\n";
        			}
        			else {
        				data+=line+" POINT<>\r\n";
        			}
        		}
        		else if(type.equals("bool")) {
        			if(line.matches(".+=(true)|(false)|1|0")) {
        				pattern = Pattern.compile("=.+");
        				matcher = pattern.matcher(line);
        				matcher.find();
        				data += line.replaceAll(matcher.group(0), "")+" BYTE "+matcher.group(0).substring(1).replaceAll("true", "1").replaceAll("false", "0")+"\r\n";
        			}
        			else {
        				data += line+"BYTE ?\r\n";
        			}
        		}
        		else if(type.equals("string")) {
        			pattern = Pattern.compile("=.*");
        			matcher = pattern.matcher(line);
        			matcher.find();
        			data += line.replaceAll(matcher.group(0), "") + " BYTE "+matcher.group(0).substring(1)+",0\r\n";
        		}
        		else if(type.matches("hex")) {
        			if(line.matches("[a-zA-Z_]\\w*")) {
        				data += line+" DWORD ?\r\n";
        			}
        			else {
        				pattern = Pattern.compile("=-?\\d[0-9a-zA-Z]+h");
        				matcher = pattern.matcher(line);
        				matcher.find();
        				line = line.replaceAll(matcher.group(0), "");
        				data += line + " DWORD "+matcher.group(0).substring(1)+"\r\n";
        			}
        		}
        		else if(type.matches("[a-zA-Z_]\\w*")) {
        			pattern = Pattern.compile("\\(.*\\)");
        			matcher = pattern.matcher(line);
        			if(matcher.find()) {
        				data+=line.replaceAll(matcher.group(0), "").replaceAll("\\(|\\)","") + " " + type + "<"+matcher.group(0).substring(1,matcher.group(0).length()-1)+">\r\n";
        			}
        			else {
        				data+=line+" "+type+"<>\r\n";
        			}
        		}
        	}
        	else if(line.matches("((#)|(\\/\\/)).*")) {
        		continue;
        	}
        	else if(line.matches("class[A-Z]+")) {
        		line = line.replaceFirst("class", "");
        		data += (currentClass = line) + " STRUCT\r\n";
        		classFields.set(currentClass, new Datatable<String,String>());
        	}
        	else if(line.matches("endclass")){
        		data += currentClass+" ENDS\r\n";
        		classSize.set(currentClass, currentClassSize);
        		currentClass = null;
        		currentClassSize = 0;
        	}
        	else if(line.matches("function[a-zA-Z_]\\w*\\(.*\\)")) {
        		line = line.replaceFirst("function", "");
        		pattern = Pattern.compile("[^\\(]+");
        		matcher = pattern.matcher(line);
        		matcher.find();
        		currentFunction = matcher.group(0);
        		line = line.replace(currentFunction, "");
        		if(line.indexOf("()")!=-1) {
        			line = line.replaceAll("\\(|\\)", "");
        			data += currentFunction+" PROTO \r\n";
        			code += currentFunction+" PROC \r\n";
        			functionsArguments.set(currentFunction, new Datatable<String,String>());
        		}
        		else {
        			line = line.replaceAll("\\(|\\)", "");
        			data += currentFunction+" PROTO ";
        			code += currentFunction+" PROC, ";
	        		String[] arguments = line.split(",");
	        		functionsArguments.set(currentFunction, new Datatable<String,String>());
	        		for(String arg:arguments) {
	        			pattern = Pattern.compile("((Image)|(string)|(Point)|(byte)|(int)|(bool)|(short)|(hex)|(function)|([A-Z]+))");
	            		matcher = pattern.matcher(arg);
	            		matcher.find();
	            		String type = matcher.group(0);
	            		arg = arg.replaceFirst(type, "");
	            		functionsArguments.get(currentFunction).set(arg, type);
	            		if(type.equals("int")||type.equals("hex"))type = "DWORD";
	            		else if(type.equals("byte")||type.equals("bool"))type = "BYTE";
	            		else if(type.equals("short"))type = "WORD";
	            		else if(type.equals("Point"))type = "DWORD";
	            		else if(type.equals("Image"))type = "DWORD";
	            		else type = "DWORD";
	            		data += arg+":"+type+",";
	            		code += arg+":"+type+",";
	        		}
	        		data = data.substring(0, data.length()-1)+"\r\n";
	        		code = code.substring(0, code.length()-1)+"\r\n";
        		}
        	}
        	else if(line.matches("endfunction")) {
        		code += currentFunction+" ENDP\r\n";
        		currentFunction = null;
        	}
        	else if(line.matches("main")) {
        		currentFunction = "main";
        		code += "main PROC\r\n";
        		functionsArguments.set(currentFunction, new Datatable<String,String>());
        	}
        	else if(line.matches("endmain")) {
        		currentFunction = null;
        		code += "main ENDP\r\nEND main\r\n";
        	}
        	else if(line.matches("[a-zA-Z_]\\w*(\\.\\w+)*((\\+\\+)|(--)|(~~))")) {
	        	pattern = Pattern.compile("[a-zA-Z_]\\w*(\\.\\w+)*(\\+|-|~)");
	        	matcher = pattern.matcher(line);
	        	matcher.find();
	        	code += "\t";
	        	String oporator = line.replace(matcher.group(0), "");
	        	String var = matcher.group(0).substring(0,matcher.group(0).length()-1);
	        	if(var.indexOf('.')!=-1&&isArgument(var.substring(0,var.indexOf('.')))&&!isPrimative(functionsArguments.get(currentFunction).get(var.substring(0,var.indexOf('.'))))){
	        		code += "mov eax, "+var.substring(0,var.indexOf('.'))+"\r\n";
	        		int ind = getArgIndex(var);
	        		if(oporator.equals("+")) {
	        			code += "\tadd [eax+"+ind+"],DWORD ptr 1\r\n";
	        		}
	        		if(oporator.equals("-")) {
	        			code += "\tsub [eax+"+ind+"],DWORD ptr 1\r\n";
	        		}
	        		if(oporator.equals("~")) {
	        			code += "\tneg DWORD ptr [eax+"+ind+"]\r\n";
	        		}
	        	}
	        	else {
	        		if(oporator.equals("+")) {
	        			code += "inc "+matcher.group(0).substring(0,matcher.group(0).length()-1)+"\r\n";
	        		}
	        		if(oporator.equals("-")) {
	        			code += "dec "+matcher.group(0).substring(0,matcher.group(0).length()-1)+"\r\n";
	        		}
	        		if(oporator.equals("~")) {
	        			code += "neg "+matcher.group(0).substring(0,matcher.group(0).length()-1)+"\r\n";
	        		}
	        	}
        	}
        	else if(line.matches("[a-zA-Z_]\\w*(\\.\\w+)*((\\+=)|(-=)|(\\^=)|(\\|=)|(&=)|=).+")) {
        		pattern = Pattern.compile("[a-zA-Z_]\\w*(\\.\\w+)*(\\+|-|\\^|=|&|\\|)");
        		matcher = pattern.matcher(line);
        		matcher.find();
        		String variable = matcher.group(0);
        		String oporator = variable.substring(variable.length()-1);
        		line = line.replaceFirst(variable, "");
        		if(!oporator.equals("="))line = line.substring(1);
        		int ind = 0;
        		boolean isArg = false;
        		if(variable.indexOf('.')!=-1&&requireChanges(variable.substring(0,variable.indexOf('.')))) {
        			ind = getArgIndex(variable.substring(0, variable.length()-1));
        			isArg = true;
        		}
        		String fixedop = null;
        		if(!oporator.equals("="))
        			line = line.substring(1);
        		if(oporator.equals("="))
        			fixedop = "mov";
        		else if(oporator.equals("+"))
        			fixedop = "add";
        		else if(oporator.equals("-"))
        			fixedop = "sub";
        		else if(oporator.equals("^"))
        			fixedop = "xor";
        		else if(oporator.equals("&"))
        			fixedop = "and";
        		else if(oporator.equals("|"))
        			fixedop = "or";
        		if(isArg) {
        			code += "\tmov edx, "+variable.substring(0, variable.indexOf('.'))+"\r\n";
        			if(line.matches("-?\\d+")) {
            			code += "\tmov [edx+"+ind+"], DWORD ptr "+line+"\r\n";
            		}
            		else if(line.matches("\\w+(\\.\\w+)*")) {
            			if(line.indexOf('.')!=-1&&requireChanges(line.substring(0,line.indexOf('.')))) {
            				int ind2 = getArgIndex(line);
            				line = line.substring(0,line.indexOf('.'));
            				code += "\tmov ebx, "+line+"\r\n";
            				code += "\tmov eax,[ebx+"+ind2+"]\r\n";
            				code += "\t"+fixedop+" [edx+"+ind+"], eax\r\n";
            			}
            			else {
            				code += "\tmov eax, "+line+"\r\n\t"+fixedop+" "+variable+", "+getRegisterBySize(variable)+"\r\n";
            			}
            		}
            		else {
            			code += breakExpretion(line)+"\t"+fixedop+" "+"[edx+"+ind+"], eax\r\n";
            		}
        		}
        		else if(line.matches("-?\\d+")) {
        			code += "\t"+fixedop+" "+variable.substring(0, variable.length()-1)+", "+line+"\r\n";
        		}
        		else if(line.matches("\\w+")) {
        			code += "\tmov "+getRegisterBySize(line)+", "+line+"\r\n\t"+fixedop+" "+variable+", "+getRegisterBySize(variable)+"\r\n";
        		}
        		else {
        			code += breakExpretion(line)+"\t"+fixedop+" "+variable.substring(0,variable.length()-1)+", eax\r\n";
        		}
        	}
        	else if(line.matches("return")) {
        		code += "\tret\r\n";
        	}
        	else if(line.matches("if\\(.+\\)")) {
        		conditionStack.push(currentCondition);
        		int tempcur = conditionStack.peek();
        		line = line.substring(3,line.length()-1);
        		if(line.matches("\\w+")) {
        			code += "\tcmp "+line+",0\r\n\tje TagNum"+currentCondition+"\r\n";
        		}
        		else if(line.matches("\\w+(\\.\\w+)+")&&!isArgument(line.substring(0,line.indexOf('.')))) {
        			code += "\tcmp "+line+",0\r\n\tje TagNum"+currentCondition+"\r\n";
        		}
        		else if(line.matches("\\w+(\\.\\w+)+")&&isArgument(line.substring(0,line.indexOf('.')))) {
        			int ind = getArgIndex(line);
        			code += "\tmov eax,"+line.substring(0,line.indexOf('.'))+"\r\n\tcmp [eax+"+ind+"],DWORD ptr 0\r\n\tje TagNum"+currentCondition+"\r\n";
        		}
        		else if(line.matches("-?\\d+")) {
        			code += "\tmov eax,"+line+"\r\n\tcmp eax,0\r\n\tje TagNum"+currentCondition+"\r\n";
        		}
        		else {
        			code += breakBoolean(line);
        			code += "\tcmp edx,0\r\n\tje TagNum"+tempcur+"\r\n";
        		}
        		currentCondition++;
        	}
        	else if(line.matches("endif")) {
        		code += "\tTagNum"+conditionStack.pop()+":\r\n";
        	}
        	// TODO add a code for the for loops
        	/*else if(line.matches("for\\(.+,.+,.+\\)")) {
        		
        	}
        	else if(line.matches("endfor")) {
        		
        	}*/
        	else if(line.matches("repeat.*")) {
        		line = line.substring(6);
        		repeatLoopNames.add("RepeatNum"+(currentRepeatLoop++));
        		if(line.matches("\\w+")) {
        			code += "\tmov ecx,"+line+"\r\n";
        		}
        		else if(line.matches("\\w+(\\.\\w+)+")&&!isArgument(line.substring(0,line.indexOf('.')))) {
        			code += "\tmov ecx,"+line+"\r\n";
        		}
        		else if(line.matches("\\w+(\\.\\w+)+")&&isArgument(line.substring(0,line.indexOf('.')))) {
        			int ind = getArgIndex(line);
        			code += "\tmov eax,"+line.substring(0,line.indexOf('.'))+"\r\n\tmov ecx,[eax+"+ind+"]\r\n";
        		}
        		else if(line.matches("-?\\d+")) {
        			code += "\tmov ecx,"+line+"\r\n";
        		}
        		else {
        			code += breakBoolean(line);
        			code += "\tmov ecx,eax\r\n";
        		}
        		code += "\t"+repeatLoopNames.peek()+":\r\n";
        		code += "\tpush ecx\r\n";
        	}
        	else if(line.matches("endrepeat")) {
        		code += "\tpop ecx\r\n";
        		code += "\tloop "+repeatLoopNames.pop()+"\r\n";
        	}
        	else if(line.matches("while\\(.+\\)")) {
        		whileLoopNames.push("WhileTagNum"+currentTag);
        		code += "\tWhileTagNum"+currentTag+":\r\n";
        		line = line.substring(6,line.length()-1);
        		if(line.matches("\\w+")) {
        			whileLoopBoolean.push("\tcmp "+line+",0\r\n\tjne WhileTagNum"+currentTag+"\r\n");
        		}
        		else if(line.matches("\\w+(\\.\\w+)+")&&!isArgument(line.substring(0,line.indexOf('.')))) {
        			whileLoopBoolean.push("\tcmp "+line+",0\r\n\tjne WhileTagNum"+currentTag+"\r\n");
        		}
        		else if(line.matches("\\w+(\\.\\w+)+")&&isArgument(line.substring(0,line.indexOf('.')))) {
        			int ind = getArgIndex(line);
        			whileLoopBoolean.push("\tmov eax,"+line.substring(0,line.indexOf('.'))+"\r\n\tcmp [eax+"+ind+"],BYTE ptr 0\r\n\tjne WhileTagNum"+currentTag+"\r\n");
        		}
        		else if(line.matches("-?\\d+")) {
        			whileLoopBoolean.push("\tmov eax,"+line+"\r\n\tcmp eax,0\r\n\tjne WhileTagNum"+currentTag+"\r\n");
        		}
        		else {
        			whileLoopBoolean.push(breakBoolean(line)+"\tcmp edx,0\r\n\tjne WhileTagNum"+currentTag+"\r\n");
        		}
        		currentTag++;
        	}
        	else if(line.matches("break\\d*")) {
        		pattern = Pattern.compile("\\d+");
        		matcher = pattern.matcher(line);
        		if(matcher.find()) {
        			Stack<String> temp = new Stack<String>();
        			int times = Integer.parseInt(matcher.group(0));
        			for(int i=0;i!=times;i++) {
        				temp.push(whileLoopNames.pop());
        			}
        			code += "\tjmp "+whileLoopNames.peek()+"\r\n";
        			while(!temp.isEmpty()) {
        				whileLoopNames.push(temp.pop());
        			}
        		}
        		else {
        			code += "\tjmp "+whileLoopNames.peek()+"\r\n";
        		}
        	}
        	else if(line.matches("endwhile")) {
        		code += whileLoopBoolean.pop();
        		code += "\tend"+whileLoopNames.pop()+":\r\n";
        	}
        	else if(line.matches("[a-zA-Z_]\\w*\\(.*\\)")) {
        		pattern = Pattern.compile("[a-zA-Z_]\\w*");
        		matcher = pattern.matcher(line);
        		matcher.find();
        		String func = matcher.group(0);
        		line = line.replace(func, "");
        		if(line.indexOf("()")!=-1) {
        			code += "\tcall "+func+"\r\n";
        		}
        		else {
	        		String[] arguments = line.substring(1,line.length()-1).split(",");
	        		String invokeargs = "";
	        		for(int i=arguments.length-1;i!=-1;i--) {
	        			if(arguments[i].indexOf('.')==-1&&!(isArgument(arguments[i])||isPrimative((String)functionsArguments.get(func).values()[i]))) {
	        				invokeargs += "\tpush offset "+arguments[i]+"\r\n";
	        			}
	        			else if(arguments[i].indexOf('.')!=-1&&!(isArgument(arguments[i].substring(0, arguments[i].indexOf('.')))||isPrimative((String)functionsArguments.get(func).values()[i]))) {
	        				invokeargs += "\tpush offset "+arguments[i]+"\r\n";
	        			}
	        			else if(arguments[i].indexOf('.')!=-1&&isArgument(arguments[i].substring(0, arguments[i].indexOf('.')))&&isPrimative((String)functionsArguments.get(func).values()[i])) {
	        				int ind = getArgIndex(arguments[i]);
	        				invokeargs += "\tmov ebx,"+arguments[i].substring(0, arguments[i].indexOf('.'))+"\r\n";
	        				invokeargs += "\tpush DWORD ptr [ebx+"+ind+"]\r\n";
	        			}
	        			else if(arguments[i].indexOf('.')!=-1&&isArgument(arguments[i].substring(0, arguments[i].indexOf('.')))) {
	        				int ind = getArgIndex(arguments[i]);
	        				invokeargs += "\tadd "+arguments[i].substring(0, arguments[i].indexOf('.'))+","+ind+"\r\n";
	        				invokeargs += "\tpush "+arguments[i].substring(0, arguments[i].indexOf('.'))+"\r\n";
	        				invokeargs += "\tsub "+arguments[i].substring(0, arguments[i].indexOf('.'))+","+ind+"\r\n";
	        			}
	        			else if(arguments[i].matches("-?\\d+")) {
	        				invokeargs += "\tpush DWORD ptr "+arguments[i]+"\r\n";
	        			}
	        			else {
	        				invokeargs += "\tpush "+arguments[i]+"\r\n";
	        			}
	        		}
	        		code += invokeargs+"\tcall "+func+"\r\n";
        		}
        	}
        	else {
        		sop(line);
        		throw new IllegalArgumentException("There were some errors in your code file");
        	}
        }
        return include+"\r\n"+data+"\r\n\r\n"+code.replaceAll("sleep", "Sleep");
    }
    private static String breakBoolean(String expression) {
		int boolnum = 0;
		Queue<Boolean> connecters = new Queue<Boolean>(); // && == true, || == false
    	Pattern p = Pattern.compile("(\\|\\||&&)");
    	Matcher m = p.matcher(expression);
    	while(m.find()) {
    		expression = expression.replaceFirst(m.group(0).equals("||")?"\\|\\|":m.group(0), "@");
    		connecters.add(m.group(0).equals("&&"));
    		m = p.matcher(expression);
    	}
    	String[] exps = expression.split("@");
    	String result = "\tmov edx,0\r\n";
    	p = Pattern.compile("==|!=|<=|>=|<|>");
    	int i = 0;
    	Boolean contype = null;
    	do{
    		if(i==exps.length) 
    			break;
    		if(contype != null) {
    			result += "\tcmp edx,0\r\n";
    			result += "\tj"+(contype.booleanValue()?"e":"ne")+" AfterCheck"+currentBoolean+"_"+boolnum+"\r\n";
    		}
    		String exp = exps[i];
    		boolean not = exp.startsWith("!");
    		if(not)exp = exp.substring(1);
    		m = p.matcher(exp);
    		m.find();
    		String op = m.group(0);
    		String[] parts = exp.split(op);
    		if(parts[0].matches("\\w+")) {
    			result += "\tmov eax,"+parts[0]+"\r\n";
    		}
    		else if(parts[0].indexOf('.')==-1) {
    			result += "\tmov eax,"+parts[0]+"\r\n";
    		}
    		else if(!isArgument(parts[0].substring(0, parts[0].indexOf('.')))) {
    			result += "\tmov eax,"+parts[0]+"\r\n";
    		}
    		else if(parts[0].matches("\\w+(\\.\\w+)+")) {
    			result += "\tmov ecx,"+parts[0].substring(0, parts[0].indexOf('.'))+"\r\n";
    			int ind = getArgIndex(parts[0]);
    			result += "\tmov eax,DWORD ptr [ecx+"+ind+"]\r\n";
    		}
    		else {
    			result += breakExpretion(parts[0]);
    		}
    		result += "\tpush eax\r\n";
    		if(parts[1].matches("\\w+")) {
    			result += "\tmov ebx,"+parts[1]+"\r\n";
    		}
    		else if(parts[1].indexOf('.')==-1) {
    			result += "\tmov ebx,"+parts[1]+"\r\n";
    		}
    		else if(!isArgument(parts[1].substring(0, parts[1].indexOf('.')))) {
    			result += "\tmov ebx,"+parts[1]+"\r\n";
    		}
    		else if(parts[1].matches("\\w+(\\.\\w+)+")) {
    			result += "\tmov ecx,"+parts[1].substring(0, parts[1].indexOf('.'))+"\r\n";
    			int ind = getArgIndex(parts[1]);
    			result += "\tmov ebx,[ecx+"+ind+"]\r\n";
    		}
    		else {
    			result += breakExpretion(parts[1],"ebx");
    		}
    		result += "\tpop eax\r\n\tcmp eax,ebx\r\n";
    		result += "\t"+(not?getComp(op):getCompOpised(getComp(op)))+" FalseInd"+currentBoolean+"_"+boolnum+"\r\n";
    		result += "\tmov edx,1\r\n\tjmp AfterCheck"+currentBoolean+"_"+boolnum+"\r\n";
    		result += "\tFalseInd"+currentBoolean+"_"+boolnum+":\r\n";
    		result += "\tmov edx,0\r\n\tAfterCheck"+currentBoolean+"_"+boolnum+":\r\n";
    		i++;
    		boolnum++;
    	}
    	while(((contype = connecters.get())!=null)||(!connecters.isEmpty()));
    	currentBoolean++;
    	return result;
	}
	
	public static String getCompOpised(String jump) {
		if(jump.equals("je"))return "jne";
		else if(jump.equals("jne"))return "je";
		else if(jump.equals("jl"))return "jge";
		else if(jump.equals("jg"))return "jle";
		else if(jump.equals("jle"))return "jg";
		else return "jl";
	}
	public static String getComp(String jump) {
		if(jump.equals("!="))return "jne";
		else if(jump.equals("=="))return "je";
		else if(jump.equals(">="))return "jge";
		else if(jump.equals("<="))return "jle";
		else if(jump.equals(">"))return "jg";
		else return "jl";
	}
	private static boolean requireChanges(String variable) {
		return isArgument(variable)&&!isPrimative(functionsArguments.get(currentFunction).get(variable));
	}

	/**
     * 
     * @param var - the full name of the argument
     * @return the binary index of the argument
     */
    private static int getArgIndex(String var) {
    	int ind = 0;
		String cls = var.substring(0,var.indexOf('.'));
		var = var.substring(cls.length()+1);
		String classname = functionsArguments.get(currentFunction).get(cls);
		do {
			try {
    			ind += getIndex(classname,var.indexOf('.')==-1?var:var.substring(0,var.indexOf('.')));
    			classname = classFields.get(classname).get(var.substring(0,var.indexOf('.')));
    			var = var.substring(var.indexOf('.')+1);
			}
			catch(Exception e){
				break;
			}
		}while(true);
		return ind;
	}

	private static String getRegisterBySize(String var) {
		return (classSize.get(var)==1?"al":classSize.get(var)==2?"ax":"eax");
	}

	private static String breakExpretion(String expression) {
		String result = "\tpush edx\r\n";
		Matcher m;
		Pattern p;
		p = Pattern.compile("[^\\^\\*\\+\\|&\\/\\%-]+");
		m = p.matcher(expression);
		m.find();
		String first = m.group(0);
		expression = expression.replace(first, "");
		if(first.indexOf('.')!=-1&&requireChanges(first.substring(0,first.indexOf('.')))) {
			int ind = getArgIndex(first);
			result += "\tmov ecx, "+first.substring(0,first.indexOf('.'))+"\r\n";
			first = "[ecx+"+ind+"]";
		}
		result += "\tmov eax, "+first+"\r\n";
    	while(expression!=null) {
			String opo = expression.substring(0, 1);
			expression = expression.substring(1);
			if(expression.matches("[^\\^\\*\\+\\|&\\/\\%-]+")) {
				String tempexp = expression;
				expression = null;
				if(tempexp.indexOf('.')!=-1&&requireChanges(tempexp.substring(0,tempexp.indexOf('.')))) {
					int ind = getArgIndex(tempexp);
					result += "\tmov ecx, "+tempexp.substring(0,tempexp.indexOf('.'))+"\r\n";
					tempexp = "DWORD ptr [ecx+"+ind+"]";
				}
				if(opo.equals("+")) {
					result += "\tadd eax,"+tempexp+"\r\n";
				}
				if(opo.equals("-")) {
					result += "\tsub eax,"+tempexp+"\r\n";
				}
				if(opo.equals("&")) {
					result += "\tand eax,"+tempexp+"\r\n";
				}
				if(opo.equals("|")) {
					result += "\tor eax,"+tempexp+"\r\n";
				}
				if(opo.equals("^")) {
					result += "\txor eax,"+tempexp+"\r\n";
				}
				if(opo.equals("*")) {
					result += "\txor edx,edx\r\n\tmul "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n";
				}
				if(opo.equals("/")) {
					result += "\txor edx,edx\r\n\tdiv "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n";
				}
				if(opo.equals("%")) {
					result += "\txor edx,edx\r\n\tdiv "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n\tmov eax,edx\r\n";
				}
			}
			else {
				m = p.matcher(expression);
				m.find();
				String tempexp = m.group(0);
				expression = expression.substring(tempexp.length());
				if(tempexp.indexOf('.')!=-1&&requireChanges(tempexp.substring(0,tempexp.indexOf('.')))) {
					int ind = getArgIndex(tempexp);
					result += "\tmov ecx, "+tempexp.substring(0,tempexp.indexOf('.'))+"\r\n";
					tempexp = "DWORD ptr [ecx+"+ind+"]";
				}
				if(opo.equals("+")) {
					result += "\tadd eax,"+tempexp+"\r\n";
				}
				if(opo.equals("-")) {
					result += "\tsub eax,"+tempexp+"\r\n";
				}
				if(opo.equals("&")) {
					result += "\tand eax,"+tempexp+"\r\n";
				}
				if(opo.equals("|")) {
					result += "\tor eax,"+tempexp+"\r\n";
				}
				if(opo.equals("^")) {
					result += "\txor eax,"+tempexp+"\r\n";
				}
				if(opo.equals("*")) {
					result += "\txor edx,edx\r\n\tmul "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n";
				}
				if(opo.equals("/")) {
					result += "\txor edx,edx\r\n\tdiv "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n";
				}
				if(opo.equals("%")) {
					result += "\txor edx,edx\r\n\tdiv "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n\tmov eax,edx\r\n";
				}
			}
		}
    	return result+"\tpop edx\r\n";
	}
	private static String breakExpretion(String expression,String register) {
		String result = "\tpush edx\r\n";
		Matcher m;
		Pattern p;
		p = Pattern.compile("[^\\^\\*\\+\\|&\\/\\%-]+");
		m = p.matcher(expression);
		m.find();
		String first = m.group(0);
		expression = expression.replace(first, "");
		if(first.indexOf('.')!=-1&&requireChanges(first.substring(0,first.indexOf('.')))) {
			int ind = getArgIndex(first);
			result += "\tmov ecx, "+first.substring(0,first.indexOf('.'))+"\r\n";
			first = "[ecx+"+ind+"]";
		}
		result += "\tmov "+register+", "+first+"\r\n";
    	while(expression!=null) {
			String opo = expression.substring(0, 1);
			expression = expression.substring(1);
			if(expression.matches("[^\\^\\*\\+\\|&\\/\\%-]+")) {
				String tempexp = expression;
				expression = null;
				if(tempexp.indexOf('.')!=-1&&requireChanges(tempexp.substring(0,tempexp.indexOf('.')))) {
					int ind = getArgIndex(tempexp);
					result += "\tmov ecx, "+tempexp.substring(0,tempexp.indexOf('.'))+"\r\n";
					tempexp = "DWORD ptr [ecx+"+ind+"]";
				}
				if(opo.equals("+")) {
					result += "\tadd "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("-")) {
					result += "\tsub "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("&")) {
					result += "\tand "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("|")) {
					result += "\tor "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("^")) {
					result += "\txor "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("*")) {
					result += "\txor edx,edx\r\n\tmul "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n\tmov "+register+", eax\r\n";
				}
				if(opo.equals("/")) {
					result += "\txor edx,edx\r\n\tdiv "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n\tmov "+register+", eax\r\n";
				}
				if(opo.equals("%")) {
					result += "\txor edx,edx\r\n\tdiv "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n\tmov "+register+",edx\r\n";
				}
			}
			else {
				m = p.matcher(expression);
				m.find();
				String tempexp = m.group(0);
				expression = expression.substring(tempexp.length());
				if(tempexp.indexOf('.')!=-1&&requireChanges(tempexp.substring(0,tempexp.indexOf('.')))) {
					int ind = getArgIndex(tempexp);
					result += "\tmov ecx, "+tempexp.substring(0,tempexp.indexOf('.'))+"\r\n";
					tempexp = "DWORD ptr [ecx+"+ind+"]";
				}
				if(opo.equals("+")) {
					result += "\tadd "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("-")) {
					result += "\tsub "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("&")) {
					result += "\tand "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("|")) {
					result += "\tor "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("^")) {
					result += "\txor "+register+","+tempexp+"\r\n";
				}
				if(opo.equals("*")) {
					result += "\txor edx,edx\r\n\tmul "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n";
				}
				if(opo.equals("/")) {
					result += "\txor edx,edx\r\n\tdiv "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n";
				}
				if(opo.equals("%")) {
					result += "\txor edx,edx\r\n\tdiv "+(tempexp.matches("-?\\d+")?"DWORD ptr ":"")+tempexp+"\r\n\tmov "+register+",edx\r\n";
				}
			}
		}
    	return result+"\tpop edx\r\n";
	}
	private static String getRegisterBySize(String var,String chr) {
		return getRegisterBySize(var).replace("a", chr);
	}
	private static boolean isArgument(String arg) {
		return currentFunction!=null&&functionsArguments.get(currentFunction).get(arg)!=null;
	}
	public static void main(String[] args){
        do{
            sop("Enter the source file");
            String PATH = reader.next();
            String compiledAsmString = compile(PATH);
            sop(compiledAsmString);
            sop("Enter the destination source");
            String DEST = reader.next();
            writeToFile(compiledAsmString,DEST);
            sop("do you want to continue?\r\nenter yes or no");
        }
        while(reader.next().charAt(0)=='y');
        return;
    }

}









/*
Replace(';',"/r/n")
Replace();



*/