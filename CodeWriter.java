import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;


/**
 * Translates VM commands into Hack assembly code,
 *  and writes it to an output .asm file.
 */
class CodeWriter {
    
    
    // the base address of the temp memory segment implementation in the hack RAM
    private static final int TEMP_BASE_ADDRESS = 5;
    
    
    //*** Data Members ***//
    
    // Writer object for writing the output hack assembly code file
    private PrintWriter writer;
    
    // The name of the currently translated .vm file (without the extension)
    private String currentFileName;
    
    // The name of the latest function declared in the input vm file
    private String currentFunctionName;
    
    // Keeping a correspondence between hack vm arithmetic commands, and hack assembly operators
    private HashMap<String, String> operatorTable;
    
    // Keeping a correspondence between vm memory segment names and predefined assembly symbols
    private HashMap<String, String> segmentSymbols;
    
    // A counter intended for generating unique labels in hack assembly language
    private int uniqueLabelNumber;
    
    
    
    /**
     * Opens the output file, and gets ready to write into it.
     * In Addition, writes the initialization (bootstrap) assembly code at the beginning of it.
     *
     * @param outputFile the output Hack assembly file.
     */
    CodeWriter(File outputFile) throws IOException {
        
        writer = new PrintWriter(outputFile);
        
        initOperatorTable();
        initSegmentSymbols();
        uniqueLabelNumber = 0;
        currentFunctionName = "";
    
        writeInit();
    }
    
    
    /**
     * @return a unique label, including the currently translated function name.
     */
    private String uniqueLabel() {
        return currentFunctionName + "." + Integer.toString(uniqueLabelNumber);
    }
    
    
    /**
     * Initializes the segmentSymbols map,
     *  with virtual memory segment names and their corresponding assembly language symbols
     *  (each symbol represents a register that holds the segment base address).
     */
    private void initSegmentSymbols() {
        segmentSymbols = new HashMap<>();
        segmentSymbols.put("local", "LCL");
        segmentSymbols.put("argument", "ARG");
        segmentSymbols.put("this", "THIS");
        segmentSymbols.put("that", "THAT");
    }
    
    
    /**
     * Initializes the operatorTable map,
     *  with arithmetic commands in vm language and their corresponding arithmetic/logical operators
     *  in Hack assembly language.
     */
    private void initOperatorTable() {
        operatorTable = new HashMap<>();
        operatorTable.put("add", "+");
        operatorTable.put("sub", "-");
        operatorTable.put("neg", "-");
        operatorTable.put("and", "&");
        operatorTable.put("or", "|");
        operatorTable.put("not", "!");
    }
    
    
    /**
     * Informs the code writer that the translation of a new VM file is started.
     *
     * @param fileName the name of the translated VM file.
     */
    void setFileName(String fileName) {
        
        currentFileName = fileName;
    }
    
    
    /**
     * Writes (to the output file) assembly code which sets the current address in Hack memory to
     *  the address of the last value pushed to the stack.
     */
    private void writeStackAccess() {
        writer.println("@SP");
        writer.println("AM=M-1");
    }
    
    
    /**
     * Writes assembly code that increments the stack pointer value.
     */
    private void incrementStackPointer() {
        writer.println("@SP");
        writer.println("M=M+1");
    }
    
    
    /**
     * Writes assembly code segment which performs comparison between the value in the D register
     *  to the last pushed element in the stack, and invokes a conditional branching command
     *  according to the result of comparison and the desired logical command.
     *
     * @param command logical command that determines the condition of the branching command.
     */
    private void writeValueComparison(String command) {
        writer.println("(COMPARE_BY_VALUE_" + uniqueLabel() + ")");
        writer.println("@SP");
        writer.println("A=M+1");
        writer.println("D=D-M");
        writer.println("@IF_TRUE_" + uniqueLabel());
        writer.println("D;J" + command.toUpperCase());
        writer.println("(IF_FALSE_" + uniqueLabel() + ")");
        writer.println("@APPEND_TO_STACK_" + uniqueLabel());
        writer.println("D=0;JMP");
        writer.println("(IF_TRUE_" + uniqueLabel() + ")");
        writer.println("D=-1");
        writer.println("(APPEND_TO_STACK_" + uniqueLabel() + ")");
    }
    
    
    /**
     * Writes assembly code which compares the signs of the two topmost values in the stack,
     *  and invokes a conditional branching command according to the result.
     *
     * @param command the desired query.
     */
    private void writeSignComparison(String command) {
        // if command equals 'gt':
        String firstJump = "JLT";
        String secondJump = "JLE";
        String thirdJump = "JGE";
    
        if (command.equals("lt")) {
            firstJump = "JGT";
            secondJump = "JGE";
            thirdJump = "JLE";
        }
        writer.println("@SECOND_CHECK_" + uniqueLabel());
        writer.println("D;" + firstJump);
        getValueFromStack();
        writer.println("@IF_FALSE_" + uniqueLabel());
        writer.println("D;" + secondJump);
        writer.println("@COMPARE_BY_VALUE_" + uniqueLabel());
        writer.println("0;JMP");
        writer.println("(SECOND_CHECK_" + uniqueLabel() + ")");
        getValueFromStack();
        writer.println("@IF_TRUE_" + uniqueLabel());
        writer.println("D;" + thirdJump);
    }
    
    
    /**
     * Writes assembly code segment that implements an equality query between the two topmost elements
     *  in the stack.
     */
    private void writeEq() {
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M-D");
        writer.println("@IF_TRUE_" + uniqueLabel());
        writer.println("D;JEQ");
        writer.println("@APPEND_TO_STACK_" + uniqueLabel());
        writer.println("D=0;JMP");
        writer.println("(IF_TRUE_" + uniqueLabel() + ")");
        writer.println("D=-1");
        writer.println("(APPEND_TO_STACK_" + uniqueLabel() + ")");
    }
    
    
    /**
     * Writes assembly code that implements the logical operation determined by the given command.
     *
     * @param command a logical query command in vm language.
     */
    private void writeLogical(String command) {
        
        if (command.equals("eq")) {
            writeEq();
        }
        else {
            writeSignComparison(command);
            writeValueComparison(command);
        }
        writeStackAssignment();
        uniqueLabelNumber++;
    }
    
    
    /**
     * Writes assembly code which implements an arithmetic binary operation determined by the given command.
     *
     * @param command an arithmetic/logical binary command in vm language.
     */
    private void writeBinaryOperation(String command) {
        
        String arithmeticOperation = "";
        
        switch (command) {
            
            case "add":
            case "and":
            case "or":
                arithmeticOperation = "M=D" + operatorTable.get(command) + "M";
                break;
            
            case "sub":
                arithmeticOperation = "M=M-D";
                break;
            
            default: writeLogical(command);
        }
        if (!arithmeticOperation.equals("")) {
            writeStackAccess();
            writer.println(arithmeticOperation);
            incrementStackPointer();
        }
    }
    
    
    /**
     * Writes (to the output file) the assembly code which is
     *  the translation of the given arithmetic command.
     *
     * @param command arithmetic command to translate.
     */
    void writeArithmetic(String command) {
        
        getValueFromStack();
        
        if (command.equals("neg") || command.equals("not")) {
            writer.println("M=" + operatorTable.get(command) + "D");
            incrementStackPointer();
        
        } else {
            writeBinaryOperation(command);
        }
    }
    
    
    /**
     * Writes assembly code that retrieves the topmost value in the stack,
     *  and assigns it to the D-register of the Hack CPU.
     */
    private void getValueFromStack() {
        writeStackAccess();
        writer.println("D=M");
    }
    
    
    /**
     * Writes (to the output file) the assembly code implementing a pop command
     *  from the stack to the given segment at the given index (entry).
     *
     * @param segment chosen vm memory segment.
     * @param index register number in the memory segment (starting at 0).
     */
    void writePop(String segment, int index) {
        
        switch (segment) {
        
            case "pointer":
                getValueFromStack();
                writePointerSegment(index);
                break;
        
            case "temp":
                getValueFromStack();
                writer.println("@" + (TEMP_BASE_ADDRESS + index));
                break;
        
            case "static":
                getValueFromStack();
                writer.println("@" + currentFileName + "." + index);
                break;
        
            default:
                // in case segment is argument/local/this/that
                popHelper(segment, index);
        }
        writer.println("M=D");
    }
    
    
    /**
     * Writes assembly code implementing a pop command from the stack to the given
     *  segment, at the entry of the given index.
     *
     * @param segment the name of a virtual memory segment in the vm.
     * @param index the index of an entry in the vm segment (non-negative integer).
     */
    private void popHelper(String segment, int index) {
        writeSegmentAddress(segment, index);
        writer.println("D=D+A");
        writer.println("@R13");
        writer.println("M=D");
        getValueFromStack();
        writer.println("@R13");
        writer.println("A=M");
    }
    
    
    /**
     * Writes assembly code implementing an addressing command
     *  to a given entry of the vm Pointer segment.
     *
     * @param index an index of an entry in the vm pointer segment (should be 0 or 1).
     */
    private void writePointerSegment(int index) {
        if (index == 0) {
            writer.println("@THIS");
        } else if (index == 1) {
            writer.println("@THAT");
        }
    }
    
    
    /**
     * Writes an assembly code segment which appends the value of the CPU D-register to the stack.
     */
    private void writeStackAssignment() {
        writer.println("@SP");
        writer.println("AM=M+1");
        writer.println("A=A-1");
        writer.println("M=D");
    }
    
    
    /**
     * Writes assembly code which implements an addressing command to a virtual memory segment
     *  of the virtual machine, in the given index.
     *
     * @param segment the vm segment to access.
     * @param index the index of the desired entry in the segment.
     */
    private void writeSegmentAddress(String segment, int index) {
        writer.println("@" + segmentSymbols.get(segment));
        writer.println("D=M");
        writer.println("@" + index);
    }
    
    
    /**
     * Writes (to the output file) the assembly code implementing a push command to the stack
     *  from the given segment at the given index (entry).
     *
     * @param segment chosen vm memory segment.
     * @param index register number in the memory segment (starting at 0).
     */
    void writePush(String segment, int index) {
        
        switch (segment) {
            case "constant":
                writer.println("@" + Integer.toString(index));
                writer.println("D=A");
                break;
                
            case "pointer": writePointerSegment(index);
                break;
                
            case "temp": writer.println("@" + (TEMP_BASE_ADDRESS + index));
                break;
                
            case "static": writer.println("@" + currentFileName + "." + index);
                break;
                
            default:
                writeSegmentAddress(segment, index);
                writer.println("A=D+A");
        }
        if (!segment.equals("constant")) {
            writer.println("D=M");
        }
        writeStackAssignment();
    }
    
    
    /**
     * Writes assembly code that effects the vm 'label' command.
     *
     * @param label the name of the label
     */
    void writeLabel(String label) {
        writer.println("(" + currentFunctionName + "$" + label + ")");
    }
    
    
    /**
     * Writes assembly code that effects the vm 'goto' command.
     *
     * @param label the name of the label
     */
    void writeGoto(String label) {
        writer.println("@" + currentFunctionName + "$" + label);
        writer.println("0;JMP");
    }
    
    
    /**
     * Writes assembly code that effects the vm 'if-goto' command.
     *
     * @param label the name of the label
     */
    void writeIf(String label) {
        getValueFromStack();
        writer.println("@" + currentFunctionName + "$" + label);
        writer.println("D;JNE");
    }
    
    
    /**
     * Writes assembly code that effects the vm 'function' command.
     *
     * @param functionName the name of the declared function
     * @param numLocals the number of local variables defined inside the function
     */
    void writeFunction(String functionName, int numLocals) {
        writer.println("(" + functionName + ")");
        
        if (numLocals > 0) {
            if (numLocals == 1) {
                    writer.println("@SP");
                    writer.println("AM=M+1");
                    writer.println("A=A-1");
                    writer.println("M=0");
            } else {
                writer.println("@" + Integer.toString(numLocals));
                writer.println("D=A");
                writer.println("@SP");
                writer.println("AM=D+M");
                writer.println("A=A-D");
                writer.println("M=0");
    
                for (int i = 1; i < numLocals; i++) {
                    writer.println("A=A+1");
                    writer.println("M=0");
                }
            }
        }
        currentFunctionName = functionName;
        uniqueLabelNumber = 0;
    }
    
    
    /**
     * Writes assembly code that effects the vm initialization, also called 'bootstrap code'.
     * This code is placed at the beginning of the output file.
     */
    private void writeInit() {
        writer.println("@256");
        writer.println("D=A");
        writer.println("@SP");
        writer.println("M=D");
        
        writer.println("@ARG");
        writer.println("M=D");
        
        writer.println("@5");
        writer.println("D=A");
        writer.println("@SP");
        writer.println("MD=D+M");
        
        writer.println("@LCL");
        writer.println("M=D");
        
        writer.println("@Sys.init");
        writer.println("0;JMP");
    }
    
    
    /**
     * Writes part of the assembly code that effects the vm 'call' command.
     * In particular, the written code effects the pushing of the pointer content to the global stack.
     *
     * @param pointerName the name of the pointer to save
     */
    private void callHelper(String pointerName) {
        writer.println("@" + pointerName);
        writer.println("D=M");
        writer.println("@SP");
        writer.println("AM=M+1");
        writer.println("M=D");
    }
    
    
    /**
     * Writes assembly code that effects the vm 'call' command.
     *
     * @param functionName the name function of the called function
     * @param numArgs the number of arguments passed to the called function
     */
    void writeCall(String functionName, int numArgs) {
        writer.println("@RET_ADDR$" + uniqueLabel());
        writer.println("D=A");
        writer.println("@SP");
        writer.println("A=M");
        writer.println("M=D");
        
        callHelper("LCL");
        callHelper("ARG");
        callHelper("THIS");
        callHelper("THAT");
        writer.println("@SP");
        writer.println("MD=M+1");
        
        writer.println("@LCL");
        writer.println("M=D");
        
        writer.println("@" + Integer.toString(numArgs));
        writer.println("D=D-A");
        writer.println("@5");
        writer.println("D=D-A");
        writer.println("@ARG");
        writer.println("M=D");
        
        writer.println("@" + functionName);
        writer.println("0;JMP");
        
        writer.println("(RET_ADDR$" + uniqueLabel() + ")");
        uniqueLabelNumber++;
    }
    
    
    /**
     * Writes part of the assembly code that effects the vm 'return' command.
     * In particular, the written assembly code restores a saved pointer content from the stack.
     *
     * @param pointerName the name of the pointer to restore
     */
    private void returnHelper(String pointerName) {
        writer.println("@R14");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("@" + pointerName);
        writer.println("M=D");
    }
    
    
    /**
     * Writes assembly code that effects the vm 'return' command.
     */
    void writeReturn() {
        writer.println("@LCL");
        writer.println("D=M");
        writer.println("@R14");
        writer.println("M=D");
        
        writer.println("@5");
        writer.println("A=D-A");
        writer.println("D=M");
        writer.println("@R15");
        writer.println("M=D");
        
        writer.println("@SP");
        writer.println("AM=M-1");
        writer.println("D=M");
        writer.println("@ARG");
        writer.println("A=M");
        writer.println("M=D");
    
        writer.println("D=A+1");
        writer.println("@SP");
        writer.println("M=D");
    
        returnHelper("THAT");
        returnHelper("THIS");
        returnHelper("ARG");
        returnHelper("LCL");
        
        writer.println("@R15");
        writer.println("A=M;JMP");
    }
    
    
    /**
     * Closes the output file.
     */
    void close() {
        writer.close();
    }
    
}
