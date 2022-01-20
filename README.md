# Nand2Tetris - VM Translator

Project #8 in "Nand to Tetris" course.
specification: https://www.nand2tetris.org/project08

Authors:
Nir Yazdi, nir.yazdi@mail.huji.ac.il
Lautaro Borrovinsky, lautaro.borrovinsky@mail.huji.ac.il


                           Project 8 - VM Translator (Program Control)
                           -------------------------------------------

Files
---------------
VMtranslator - small script for executing purposes.

Code Files:

VMtranslator.java - The main module of the program.
                    Implements a stack-based VM Translator for the Hack computer platform,
                    focusing on stack arithmetic and memory access commands.

Parser.java - Parser class. Handles the parsing and encapsulating of an input vm code file.

CodeWriter.java - A class intended for translating VM commands into Hack assembly code,
                   and writing it to the output .asm file.

VMsyntaxException.java - An Exception class for indicating a syntactic error in vm code.


Remarks
-------
In the CodeWriter module, we needed to translate the logical binary commands 'gt' and 'lt',
in a way that will result in a correct boolean result for every valid input.
Specifically, for inputs that may cause an overflow when subtracting one of the values from the other,
we first check the sign of each number, and determine a result or an invocation of a value comparison
in the following way:

Say we want to query x > y, meaning x and y are the two topmost values in the stack,
and we encounter the command 'gt'.

In that case:

1) If y < 0, then:
   If x >= 0, we conclude that x > y, and thus we push the value -1 (representing 'true') to the stack.
   Otherwise, x is also negative, and there is no risk of overflow in subtracting y from x,
   so we will simply check whether x - y > 0.

2) If y >= 0, then:
   If x <= 0, we conclude that x <= 0, meaning x isn't greater then y. thus, in that case we push the
   value 0 (representing 'false') to the stack.
   Otherwise, x is strictly positive, and we imply that |x - y| < 2^15, so we
   simply check whether x - y > 0, without risking overflow in doing that.

The handling of 'lt' command is symmetric,
as described in the method 'writeSignComparison' that implements the above logic.


In addition, the bootstrap code added in the beginning of the output file (by the method writeInit())
includes assembly code which calls the function Sys.init.
However, in contrast to regular translation of call commands in VM code, there is no caller frame to save.
Hence, the bootstrap code only imitates saving a caller frame (pushing segment pointers to the stack)
by increasing the value of SP by 5.
