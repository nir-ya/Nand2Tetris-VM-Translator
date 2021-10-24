import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Handles the parsing of a single virtual machine code file (.vm file),
 *  and encapsulates access to the input code.
 * Reads VM commands, parses them, and provides convenient access to their components.
 */
class Parser {
    
    
    // A regular expression describing white spaces
    private static final String BLANK_LINE = "\\s*";
    
    // A regular expression describing a comment in the vm language
    private static final String COMMENT = "(\\s*//.*)";
    
    // A regular expression describing an arithmetic command in the vm language
    private static final String ARITHMETIC_COMMAND = "\\s*(?<arg1>add|sub|neg|eq|gt|lt|and|or|not)\\s*";
    
    // A regular expression describing a virtual memory segment name (in the virtual machine)
    private static final String SEGMENT_NAME =
                                            "(?<arg1>argument|local|static|constant|this|that|pointer|temp)";
    
    // A regular expression describing a memory entry index (a non-negative integer)
    private static final String SEGMENT_ENTRY_INDEX = "(?<arg2>\\d+)";
    
    // A regular expression describing a memory access command in the vm language
    private static final String PUSH_POP_COMMAND = "\\s*(?<command>push|pop)\\s+"
                                                    + SEGMENT_NAME + "\\s+" + SEGMENT_ENTRY_INDEX + "\\s*";
    
    // Regular expressions describing label/function names
    private static final String LABEL_CHAR = "[\\w.:]";
    private static final String LABEL = "(?<arg1>([\\D&&" + LABEL_CHAR + "]" + LABEL_CHAR + "*))\\s*";
    private static final String FUNC_NAME = "(?<arg1>([\\D&&" + LABEL_CHAR + "]" + LABEL_CHAR + "*))";
    
    // A regular expression describing a program flow command in the vm language (label / goto / if-goto)
    private static final String PROGRAM_FLOW_COMMAND = "\\s*(?<command>label|goto|if-goto)\\s+" + LABEL;
    
    // Regular expressions describing function and call commands
    private static final String FUNCTION_COMMAND = "\\s*function\\s+" + FUNC_NAME + "\\s+(?<arg2>\\d+)\\s*";
    private static final String CALL_COMMAND = "\\s*call\\s+" + FUNC_NAME + "\\s+(?<arg2>\\d+)\\s*";
    
    // Patterns for spotting and matching virtual machine commands (described above)
    private static final Pattern commentPattern = Pattern.compile(COMMENT);
    private static final Pattern arithmeticPattern = Pattern.compile(ARITHMETIC_COMMAND + COMMENT + "?");
    private static final Pattern pushPopPattern = Pattern.compile(PUSH_POP_COMMAND + COMMENT + "?");
    private static final Pattern programFlowPattern = Pattern.compile(PROGRAM_FLOW_COMMAND + COMMENT + "?");
    private static final Pattern functionPattern = Pattern.compile(FUNCTION_COMMAND + COMMENT + "?");
    private static final Pattern callPattern = Pattern.compile(CALL_COMMAND + COMMENT + "?");
    private static final Pattern returnPattern = Pattern.compile("\\s*return\\s*" + COMMENT + "?");
    
    
    //*** Data Members ***//
    
    // Buffered reader for parsing the input vm code file.
    private BufferedReader reader;
    
    // Current line in the input code file.
    private String currentLine;
    
    // The number of currently read line in the file (starting count at 1).
    private int lineNumber;
    
    // a matcher object for matching VM commands and accessing their underlying fields.
    private Matcher commandMatcher;
    
    // The name of the input vm code file to parse.
    private String inputFilename;
    
    
    /**
     * Class constructor.
     * Opens the input file/stream and gets ready to parse it
     * (advances the parser to the first command in the code).
     *
     * @param inputFile the vm file to parse.
     */
    Parser(File inputFile) throws IOException {
        
        reader = Files.newBufferedReader(inputFile.toPath());
        inputFilename = inputFile.getName();
        lineNumber = 0;
        advance();
    }
    
    
    /**
     * Advances the reader to the next line in the input code file.
     *
     * @throws IOException in case of a problem handling the input file.
     */
    private void readNextLine() throws IOException {
        currentLine = reader.readLine();
        lineNumber++;
    }
    
    
    /**
     * Reads the next command from the input and makes it the current command
     * (skips white spaces and comment lines).
     *
     * @throws IOException in case of a problem handling the input file.
     */
    void advance() throws IOException {
        readNextLine();
    
        while (currentLine != null
                && (currentLine.matches(BLANK_LINE) || commentPattern.matcher(currentLine).matches())) {
            readNextLine();
        }
    }
    
    
    /**
     * Returns true iff there are more VM commands in the input code file.
     * (commands refer to code lines which aren't empty and aren't comment lines).
     *
     * @return true if there are more commands in the input code, false otherwise.
     */
    boolean hasMoreCommands() {
        return currentLine != null;
    }
    
    
    /**
     * Represents a vm language command (arithmetic / memory access / program control).
     */
    enum Command{C_ARITHMETIC, C_PUSH, C_POP, C_LABEL, C_GOTO, C_IF, C_FUNCTION, C_CALL, C_RETURN}
    
    
    /**
     * Returns the type of the current VM command in the input file.
     * C_ARITHMETIC is returned for all arithmetic commands.
     *
     * @return a Command constant, corresponding to the vm command in the currently read line.
     */
    Command commandType() throws VMsyntaxException {
        
        if ((commandMatcher = arithmeticPattern.matcher(currentLine)).matches()) {
            return Command.C_ARITHMETIC;
        
        } else if ((commandMatcher = pushPopPattern.matcher(currentLine)).matches()) {
            switch (commandMatcher.group("command")) {
                case "push": return Command.C_PUSH;
                default: return Command.C_POP;
            }
            
        } else if ((commandMatcher = programFlowPattern.matcher(currentLine)).matches()) {
            switch (commandMatcher.group("command")) {
                case "label": return Command.C_LABEL;
                case "goto": return Command.C_GOTO;
                default: return Command.C_IF;
            }
            
        } else if ((commandMatcher = functionPattern.matcher(currentLine)).matches()) {
            return Command.C_FUNCTION;
        
        } else if ((commandMatcher = callPattern.matcher(currentLine)).matches()) {
            return Command.C_CALL;
    
        } else if ((commandMatcher = returnPattern.matcher(currentLine)).matches()) {
            return Command.C_RETURN;
        }
        
        else {
            throw new VMsyntaxException("Syntax error in line "
                                        + lineNumber + " of the file " + inputFilename + ": " + currentLine);
        }
    }
    
    
    /**
     * Returns the first argument of the current command.
     * In case of C_ARITHMETIC, the command itself (add, sub, etc.) is returned.
     * In case of C_PUSH/C_POP, the name of the segment is returned.
     * In case of C_LABEL/C_GOTO/C_IF, the label is returned.
     * In case of C_FUNCTION/C_CALL the name of the function is returned.
     *
     * Should not be called if the current command is C_RETURN.
     *
     * @return the first argument of the current command.
     */
    String arg1() {
        
        return commandMatcher.group("arg1");
    }
    
    
    /**
     * Returns the second argument of the current command, if such exists.
     * Called only if the current command is C_PUSH, C_POP, C_FUNCTION or C_CALL.
     *
     * @return the second argument of the current command (an integer).
     */
    int arg2() {
        
        return Integer.parseInt(commandMatcher.group("arg2"));
    }
    
    
    /**
     * Closes the reader object of the input file.
     *
     * @throws IOException in case of an I/O problem with closing the input file.
     */
    void cleanUp() throws IOException {
        reader.close();
    }
}
