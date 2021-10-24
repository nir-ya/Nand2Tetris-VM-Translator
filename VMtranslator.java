import java.io.File;
import java.io.IOException;


/**
 * An implementation of a stack-based Virtual Machine Translator for the Hack computer platform.
 * The VM Translator is designed to translate VM code into Hack assembly code,
 *  and does that by receiving an input VM program and
 *  yielding a corresponding program written in the Hack assembly language.
 * The translated code conforms to the standard VM mapping on the Hack platform.
 */
public class VMtranslator {
    
    
    // The index of source file name in command line arguments array
    private static final int ARGUMENT_INDEX = 0;
    
    // The extension (type) of the output assembly code file
    private static final String OUTPUT_FILE_EXTENSION = ".asm";
    
    // The extension (type) of an input vm code file
    private static final String INPUT_FILES_EXTENSION = ".vm";
    
    
    //*** Data Members ***//
    
    // Parser object for parsing an input vm code file
    private Parser parser;
    
    // Writer object for creating and writing the output assembly code file
    private CodeWriter writer;
    
    // The file/directory of the vm program to translate
    private File sourceToTranslate;
    
    
    
    /**
     * Class constructor. creates a new VMtranslator of the input source (directory/file).
     * @param source the source to translate.
     */
    private VMtranslator(File source) {
        sourceToTranslate = source;
    }
    
    
    /**
     * Creates a new assembly code (.asm) file with the input name,
     *  in the same directory of the input source (given to the translator).
     * If a file of this name already exists in the input directory, overwrites it.
     *
     * @param name the pathname of the output file to create (without extension).
     * @return the output File object, representing the newly created .vm file.
     * @throws IOException in case of an I/O problem with creating the file.
     */
    private File createOutputFile(String name) throws IOException {
        
        File outputFile = new File(name + OUTPUT_FILE_EXTENSION);
        
        if (!outputFile.createNewFile()) {
            System.out.println("The program is overwriting " + outputFile.getName());
        }
        return outputFile;
    }
    
    
    /**
     * Parses the current line in the currently translated .vm file,
     * and writes the corresponding assembly code to the output file (its translation to assembly).
     *
     * @throws VMsyntaxException in case of a syntactic error found in the vm input code.
     */
    private void writeNextLine() throws VMsyntaxException {
    
        Parser.Command commandType = parser.commandType();
        
        switch (commandType) {
            
            case C_ARITHMETIC: writer.writeArithmetic(parser.arg1());
                break;
            case C_PUSH: writer.writePush(parser.arg1(), parser.arg2());
                break;
            case C_POP: writer.writePop(parser.arg1(), parser.arg2());
                break;
                
            case C_LABEL: writer.writeLabel(parser.arg1());
                break;
            case C_GOTO: writer.writeGoto(parser.arg1());
                break;
            case C_IF: writer.writeIf(parser.arg1());
                break;
                
            case C_FUNCTION: writer.writeFunction(parser.arg1(), parser.arg2());
                break;
            case C_CALL: writer.writeCall(parser.arg1(), parser.arg2());
                break;
            case C_RETURN: writer.writeReturn();
                break;
        }
    }
    
    
    /**
     * Executes the translation of the input .vm file to an assembly code file,
     *  created in the same directory.
     *
     * @param sourceFile the currently translated .vm file.
     * @throws IOException in case of an error handling the input/output file.
     * @throws VMsyntaxException in case of a syntactic error in the input vm code.
     */
    private void translateFile(File sourceFile) throws IOException, VMsyntaxException {
    
        writer.setFileName(trimExtension(sourceFile.getName()));
        
        parser = new Parser(sourceFile);
    
        while (parser.hasMoreCommands()) {
        
            writeNextLine();
            parser.advance();
        }
        parser.cleanUp();
    }
    
    
    /**
     * Executes the translation of all the .vm files in a given directory to a single assembly code file,
     *  which is created in the input directory, and named after it.
     *
     * @param sourceDir the directory containing one or more .vm files.
     * @throws IOException in case of an error handling the input/output files.
     * @throws VMsyntaxException in case of a syntactic error in a vm code file.
     */
    private void translateDirectory(File sourceDir) throws IOException, VMsyntaxException {
        
        String outputPath = sourceDir.getAbsolutePath() + File.separator + sourceDir.getName();
        writer = new CodeWriter(createOutputFile(outputPath));
        
        File[] vmFiles = sourceDir.listFiles(pathname -> pathname.isFile()
                                             && pathname.getName().endsWith(INPUT_FILES_EXTENSION));
        
        if (vmFiles != null) {
            for (File currentFile : vmFiles) {
                translateFile(currentFile);
            }
        }
    }
    
    
    /**
     * Returns the input pathname, without its extension.
     * @param pathname the pathname to trim.
     * @return the input pathname, without extension.
     */
    private static String trimExtension(String pathname) {
        
        return pathname.substring(0, pathname.lastIndexOf("."));
    }
    
    
    /**
     * Executes the translation process of the input directory/file.
     * If the input is a single (.vm) file containing a virtual machine program,
     *  translates it to an output text file containing the corresponding Hack assembly code.
     * If the input is a directory,
     *  translates all the .vm files in the given directory to a single .asm text file,
     *  containing the corresponding Hack assembly code.
     *
     * @throws IOException in case of an i/o problem with handling the input or output files.
     * @throws VMsyntaxException in case of a syntactic error in the input vm code.
     */
    private void execute() throws IOException, VMsyntaxException {
        
        try {
            if (sourceToTranslate.isFile()) {
                
                writer = new CodeWriter(createOutputFile(trimExtension(sourceToTranslate.getAbsolutePath())));
                translateFile(sourceToTranslate);
            }
            else if (sourceToTranslate.isDirectory()) {
                
                translateDirectory(sourceToTranslate);
            }
        }
        finally {
            if (parser != null) {
                parser.cleanUp();
            }
            writer.close();
        }
    }
    
    
    /**
     * Creates a File object from a given source pathname, and ensures its validity (if it exists).
     *
     * @param pathname the pathname (relative or absolute) of the input file/directory to translate.
     * @return the newly created File object that represents the input source.
     * @throws IllegalArgumentException in case a file/directory of the given pathname doesn't exist.
     */
    private static File createSource(String pathname) throws IllegalArgumentException {
        File source = new File(pathname);
        
        if (!source.exists()) {
            throw new IllegalArgumentException("The input argument '" + source.getName()
                    + "' isn't a valid file or directory!");
        }
        
        return source;
    }
    
    
    
    /**
     * Accepts a single command line argument
     * (pathname of a .vm file, or a directory containing one or more .vm files),
     * And outputs a single Hack assembly code file which is the translation of the input source,
     *  created in the same directory as the input.
     *
     * @param args a single argument (String),
     *             which is the name (pathname, relative or absolute) of an input file or directory.
     */
    public static void main(String[] args) {
        
        File input = createSource(args[ARGUMENT_INDEX]);
        VMtranslator translator = new VMtranslator(input);
        
        try {
            translator.execute();
        }
        catch (IOException e) {
            System.err.println("I/O Problem: " + e.getMessage());
        }
        catch (VMsyntaxException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }
}
