package carpet.script;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;


/**
 * <h1>Scarpet programming language</h1>
 *
 * <p>Scarpet (a.k.a. Carpet Script, or Script for Carpet) is a programming language designed to provide
 * the ability to write custom programs to run within Minecraft and
 * interact with the world.</p>
 *
 * <p>The project was initially built based on the EvalEx project,
 * however it now diverged so far away from the original implementation,
 * it would be hard to tell it without this mention.
 * EvalEx is a handy expression evaluator for Java, that
 * allows to evaluate simple mathematical and boolean expressions.
 * For more information, see:
 * <a href="https://github.com/uklimaschewski/EvalEx">EvalEx GitHub
 * repository</a></p>
 *
 * <p>This specification is divided into two sections: this one is agnostic
 * to any Minecraft related features and could function on its own, and {@see carpet.script.CarpetExpression} for
 * Minecraft specific routines and world manipulation functions.</p>
 *
 * <h1>Synopsis</h1>
 *
 * <pre>
 * script run print('Hello World!')
 * </pre>
 * <p>or an overly complex example:</p>
 * <pre>
 * /script run
 * $    block_check(x1, y1, z1, x2, y2, z2, block_to_check) ->
 * $    (
 * $        l(minx, maxx) = sort(l(x1, x2));
 * $        l(miny, maxy) = sort(l(y1, y2));
 * $        l(minz, maxz) = sort(l(z1, z2));
 * $        'Need to compute the size of the area of course';
 * $        'Cause this language doesn\'t support comments';
 * $        xsize = maxx - minx + 1;
 * $        ysize = maxy - miny + 1;
 * $        zsize = maxz - minz + 1;
 * $        total_count = 0;
 * $        loop(xsize,
 * $            xx = minx + _ ;
 * $            loop(ysize,
 * $                yy = miny + _ ;
 * $                loop(zsize,
 * $                    zz = minz + _ ;
 * $                    if ( block(xx,yy,zz) == block_to_check,
 * $                        total_count += ceil(rand(1))
 * $                    )
 * $                )
 * $            )
 * $        );
 * $        total_count
 * $    );
 * $    check_area_around_closest_player_for_block(block_to_check) ->
 * $    (
 * $        closest_player = players();
 * $        l(posx, posy, posz) = query(closest_player, 'pos');
 * $        total_count = block_check( posx-8,1,posz-8, posx+8,17,posz+8, block_to_check);
 * $        print('There is '+total_count+' of '+block_to_check+' around you')
 * $    )
 *
 * /script invoke check_area_around_closest_player_for_block 'diamond_ore'
 * </pre>
 * <p>or simply</p>
 * <pre>
 * script run print('There is'+for(rect(x,9,z,8,8,8), _ == 'diamond_ore')+' diamond ore around you')
 * </pre>
 * <p>It definitely pays to check what higher level <code>scarpet</code> functions have to offer</p>
 * <h1>Programs</h1>
 * <p>
 * You can think of an program like a mathematical expression, like
 * <code>"2.4*sin(45)/(2-4)"</code> or  <code>"sin(y)&gt;0 &amp; max(z, 3)&gt;3"</code>
 * Writing a program, is like writing a <code>2+3</code>, just a bit longer</p>
 *
 * <h2>Basic language components</h2>
 * <p>Programs consist of constants, like <code>2</code>, <code>3.14</code>, <code>pi</code>, or <code>'foo'</code>,
 * operators like <code>+</code>, <code>/</code>, <code>-&gt;</code>, variables which you can define, like <code>foo</code>
 * or special ones that will be defined for you, like <code>_x</code>, or <code>_</code> , which I specific to
 * a each built in function, and functions with name, and arguments in the form of <code>f(a,b,c)</code>, where
 * <code>f</code> is the function name, and <code>a, b, c</code> are the arguments which can be any other expression.
 * And that's all the parts of the language, so all in all - sounds quite simple.</p>
 *
 * <h2>Strings</h2>
 *
 * <h2>Code flow</h2>
 * <p>
 *     Like any other proper programming language, <code>scarpet</code> needs brackets, basically to identify
 *     where stuff begins and where it ends. In the languages that uses much more complicated constructs, like Java,
 *     they tend to use all sort of them, round ones to indicate function calls, curly to indicate section of code,
 *     square to access lists, pointy for generic types etc... I mean - there is no etc, cause they have exhausted
 *     all the bracket options...
 * </p>
 * <p><code>Scarpet</code> is different, since it runs everything based on functions (although its not per se a functional language like lisp)
 * only needs the round brackets for everything, and it is up to the programmer to organize its code so its readable,
 * as adding more brackets does not have any effect on the performance of the programs as they are compiled before they are executed.
 * Look at the following example usage of <code>if()</code> function:
 * </p>
 * <pre>
 * if(x&lt;y+6,set(x,8+y,z,'air');plop(x,top('surface',x,z),z,'birch'),sin(query(player(),'yaw'))&gt;0.5,plop(0,0,0,'boulder'),particle(x,y,z,'fire'))
 * </pre>
 * <p>Would you prefer to read</p>
 * <pre>
 * if(   x&lt;y+6,
 *            set(x,8+y,z,'air');
 *            plop(x,top('surface',x,z),z,'birch'),
 *       sin(query(player(),'yaw'))&gt;0.5,
 *            plop(0,0,0,'boulder'),
 *       particle(x,y,z,'fire')
 * )
 * </pre>
 * <p>Or rather:</p>
 * <pre>
 * if
 * (
 *     x&lt;y+6,
 *     (
 *         set(x,8+y,z,'air');
 *         plop(x,top('surface',x,z),z,'birch')
 *     ),
 *
 *     sin(query(player(),'yaw'))&gt;0.5,
 *     (
 *         plop(0,0,0,'boulder')
 *     ),
 *
 *     particle(x,y,z,'fire')
 * )
 * </pre>
 * <p>Whichever style you prefer it doesn't matter. It typically depends on the situation and the complexity of the
 * subcomponents. No matter how many whitespaces and extra brackets you add - the code will evaluate to exactly the
 * same expression, and will run exactly the same, so make sure your programs are nice and clean so others don't
 * have problems with them</p>
 *
 * <h2>Functions and scoping</h2>
 * <p>
 * Users can define functions in the form <code>fun(args....) -&gt; expression </code> and they are compiled and saved
 * for further execution in this but also subsequent calls of /script command. This means that once defined functions
 * are saved with the world for further use. There are two types of variables,
 * global - which are shared anywhere in the code, and those are all which name starts with 'global_', and
 * local variables which is everything else and those are only visible inside each function.
 * This also means that all the parameters in functions are passed 'by value', not 'by reference'.
 * </p>
 *
 * <h2>Outer variables</h2>
 * <p>Functions can still 'borrow' variables from the outer scope,
 * by adding them to the function signature wrapped around built-in function <code>outer</code>. What this does is
 * it borrows the reference to that variable from the outer scope to be used inside the function and any modifications to that outer
 * variable would result in changes of that value in the outer scope as well. Its like passing the parameters by reference,
 * except the calling function itself decides what variables its borrowing and what its name. This can be used to
 * return more than one result from a function call, although its a very ugly way of doing it -
 * I would still recommend returning a list of values instead.
 * Variables from outer scopes have a similar behaviour to, for example, <code>nonlocal</code> variables from python. </p>
 *
 *
 * <h2>Line indicators</h2>
 * <p>Since the maximum command that can be input to the chat is limited in length, you will be probably inserting your
 * programs by pasting them to command blocks, however pasting to command blocks will remove some whitespaces and squish
 * your newlines making the code not readable. If you are pasting a program that is perfect and will never cause an error,
 * I salute you, but for the most part it is quite likely that your program might break, either at compile time, when
 * its initially analyzed, or at execute time, when you suddenly attempt to divide something by zero. In these cases
 * you would want to get a meaningful error message, but for that you would need to indicate for the compiler where
 * did you put these new lines, since command block would squish them. For that, place  at the beginning
 * of the line to let the copiler know where are you. This makes so that <code>$</code> is the only character that is
 * illegal in programs, since it will be replaced with new lines. As far as I know, <code>$</code> is not used
 * anywhere inside Minecraft identifiers, so this shoudn't hinder the abilities of your programs.</p>
 * <p>Consider the following program executed as command block command:</p>
 * <pre>
 * /script run
 * run_program() -&gt; (
 *   loop( 10,
 *     foo = floor(rand(10));
 *     check_not_zero(foo);
 *     print(_+' - foo: '+foo);
 *     print('  reciprocal: '+  _/foo )
 *   )
 * );
 * check_not_zero(foo) -&gt; (
 *   if (foo==0, foo = 1)
 * )
 * </pre>
 * <p>Lets say that the intention was to check if the bar is zero and prevent division by zero in print,
 * but because the <code>foo</code> is passed as a variable, it never changes the original foo value.
 * Because of the inevitable division by zero, we get the following message:
 * </p>
 * <pre>
 * Your math is wrong, Incorrect number format for Infinity at pos 112
 * run_program() -&gt; (  loop( 10,    foo = floor(rand(10));    check_not_zero(foo);
 * print(_+' - foo: '+foo);     HERE&gt;&gt; print('  reciprocal: '+  _/foo )  ));
 * check_not_zero(foo) -&gt; (  if (foo==0, foo = 1))
 * </pre>
 *
 * As we can see, we got our problem where the result of the mathematical operation was not a number (<code>Infinity</code>, so not a number),
 * however by pasting our program
 * into the command made it squish the newlines so while it is clear where the error happened and we still can track the error down,
 * the position of the error (112) is not very helpful and wouldn't be useful if the program gets significantly longer.
 * To combat this issue we can precede every line of the script with dollar signs <code>$</code>:
 * <pre>
 * /script run
 * $run_program() -&gt; (
 * $  loop( 10,
 * $    foo = floor(rand(10));
 * $    check_not_zero(foo);
 * $    print(_+' - foo: '+foo);
 * $    print('  reciprocal: '+  _/foo )
 * $  )
 * $);
 * $check_not_zero(foo) -&gt; (
 * $  if (foo==0, foo = 1)
 * $)
 * </pre>
 *
 * <p>Then we get the following error message</p>
 *
 * <pre>
 * Your math is wrong, Incorrect number format for Infinity at line 7, pos 5
 *     print(_+' - foo: '+foo);
 *      HERE&gt;&gt; print('  reciprocal: '+  _/foo )
 *   )
 * </pre>
 *
 *
 * <p>As we can note not only we get much more concise snippet, but also information about the line
 * number and position, so means its way easier to locate the potential problems problem</p>
 *
 * <p>Obviously that's not the way we intended this program to work. To get it <code>foo</code> modified via
 * a function call, we would either return it as a result and assign it to the new variable:
 * </p>
 * <pre>
 *     foo = check_not_zero(foo);
 *     ...
 *     check_not_zero(foo) -&gt; if(foo == 0, 1, foo)
 * </pre>
 * <p>.. or convert it to a global variable, which in this case passing as an argument is not required</p>
 * <pre>
 *     global_foo = floor(rand(10));
 *     check_foo_not_zero();
 *     ...
 *     check_foo_not_zero() -&gt; if(global_foo == 0, global_foo = 1)
 * </pre>
 * <p>.. or scope foo from the outer function - in this case the inner function has to determine what it is accessing</p>
 * <pre>
 *     foo = floor(rand(10));
 *     check_foo_not_zero();
 *     ...
 *     check_foo_not_zero(outer(foo)) -&gt; if(foo == 0, foo = 1)
 * </pre>
 *
 *<p><code>outer</code> scope can only be used in
 * function signatures to indicate outer variables. They are not arguments, but still you would want to use
 * locally without affecting other uses of foo in your program.
 * </p>
 *
 *
 *
 * <p>For the most part - passing arguments as values, and using returned values   .
 * The main usecase of <code>Scarpet</code> would rather be simpler scripts, default scope for all variables is global, unless variable
 * is declared with <code>local</code> scope explicitly.
 * </p>
 */

public class Expression implements Cloneable
{
    private static final Map<String, Integer> precedence = new HashMap<String,Integer>() {{
        put("unary+-!", 60);
        put("exponent^", 40);
        put("multiplication*/%", 30);
        put("addition+-", 20);
        put("compare>=><=<", 10);
        put("equal==!=", 7);
        put("and&&", 5);
        put("or||", 4);
        put("assign=<>", 3);
        put("def->", 2);
        put("nextop;", 1);
    }};
    private static final Random randomizer = new Random();

    private static final Value PI = new NumericValue(
            "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

    private static final Value euler = new NumericValue(
            "2.71828182845904523536028747135266249775724709369995957496696762772407663");

    // %[argument_index$][flags][width][.precision][t]conversion
    private static Pattern formatPattern = Pattern.compile("%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])");

    /** The current infix expression */
    private String expression;
    String getCodeString() {return expression;}

    private String name;
    String getName() {return name;}
    Expression withName(String alias)
    {
        name = alias;
        return this;
    }

    /** Cached AST (Abstract Syntax Tree) (root) of the expression */
    private LazyValue ast = null;

    /** script specific operatos and built-in functions */
    private Map<String, ILazyOperator> operators = new HashMap<>();
    boolean isAnOperator(String opname) { return operators.containsKey(opname) || operators.containsKey(opname+"u");}

    private Map<String, ILazyFunction> functions = new HashMap<>();

    static final Map<String, UserDefinedFunction> globalFunctions = new HashMap<>();

    static final Map<String, LazyValue> globalVariables = new HashMap<String, LazyValue>() {{
        put("euler", (c, t) -> euler);
        put("pi", (c, t) -> PI);
        put("null", (c, t) -> Value.NULL);
        put("true", (c, t) -> Value.TRUE);
        put("false", (c, t) -> Value.FALSE);

        //special variables for second order functions so we don't need to check them all the time
        put("_", (c, t) -> new NumericValue(0).bindTo("_"));
        put("_i", (c, t) -> new NumericValue(0).bindTo("_i"));
        put("_a", (c, t) -> new NumericValue(0).bindTo("_a"));
    }};

    /* should the evaluator output value of each ;'s statement during execution */
    private Consumer<String> logOutput = null;
    Consumer<String> getLogger() {return logOutput;}
    void setLogOutput(Consumer<String> to) { logOutput = to; }

    @Override
    protected Expression clone() throws CloneNotSupportedException
    {
        // very very shallow copy for global functions to grab the context for error msgs
        Expression copy = (Expression) super.clone();
        copy.expression = this.expression;
        copy.name = this.name;
        return copy;
    }

    /* The expression evaluators exception class. */
    static class ExpressionException extends RuntimeException
    {
        ExpressionException(String message)
        {
            super(message);
        }

        private static TriFunction<Expression, Tokenizer.Token, String, List<String>> errorMaker = (expr, token, errmessage) ->
        {

            List<String> snippet = getExpressionSnippet(token, expr.expression);
            List<String> errMsg = new ArrayList<>(snippet);
            if (snippet.size() != 1)
            {
                errmessage+= " at line "+(token.lineno+1)+", pos "+(token.linepos+1);
            }
            else
            {
                errmessage += " at pos "+(token.pos+1);
            }
            if (expr.name != null)
            {
                errmessage += " ("+expr.name+")";
            }
            errMsg.add(errmessage);
            return errMsg;
        };
        static TriFunction<Expression, Tokenizer.Token, String, List<String>> errorSnooper = null;

        static String makeMessage(Expression e, Tokenizer.Token t, String message) throws ExpressionException
        {
            if (errorSnooper != null)
            {
                List<String> alternative = errorSnooper.apply(e, t, message);
                if (alternative!= null)
                {
                    return String.join("\n", alternative);
                }
            }
            return String.join("\n", errorMaker.apply(e, t, message));
        }

        ExpressionException(Expression e, Tokenizer.Token t, String message)
        {
            super(makeMessage(e, t, message));
        }
    }
    /* The internal expression evaluators exception class. */
    static class InternalExpressionException extends ExpressionException
    {
        InternalExpressionException(String message)
        {
            super(message);
        }
    }
    /* Exception thrown to terminate execution mid expression (aka return statement) */
    static class ExitStatement extends RuntimeException
    {
        Value retval;
        ExitStatement(Value value)
        {
            retval = value;
        }
    }
    static class ReturnStatement extends ExitStatement
    {

        ReturnStatement(Value value)
        {
            super(value);
        }
    }
    static class ThrowStatement extends ExitStatement
    {
        ThrowStatement(Value value)
        {
            super(value);
        }
    }


    static List<String> getExpressionSnippet(Tokenizer.Token token, String expr)
    {

        List<String> output = new ArrayList<>();
        for (String line: getExpressionSnippetLeftContext(token, expr, 1))
        {
            output.add(line);
        }
        List<String> context = getExpressionSnippetContext(token, expr);
        output.add(context.get(0)+" HERE>> "+context.get(1));
        for (String line: getExpressionSnippetRightContext(token, expr, 1))
        {
            output.add(line);
        }
        return output;
    }

    private static List<String> getExpressionSnippetLeftContext(Tokenizer.Token token, String expr, int contextsize)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length == 1) return output;
        for (int lno=token.lineno-1; lno >=0 && output.size() < contextsize; lno-- )
        {
            output.add(lines[lno]);
        }
        Collections.reverse(output);
        return output;
    }

    private static List<String> getExpressionSnippetContext(Tokenizer.Token token, String expr)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length > 1)
        {
            output.add(lines[token.lineno].substring(0, token.linepos));
            output.add(lines[token.lineno].substring(token.linepos));
        }
        else
        {
            output.add( expr.substring(max(0, token.pos-40), token.pos));
            output.add( expr.substring(token.pos, min(token.pos+1+40, expr.length())));
        }
        return output;
    }

    private static List<String> getExpressionSnippetRightContext(Tokenizer.Token token, String expr, int contextsize)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length == 1) { return output; }
        for (int lno=token.lineno+1; lno < lines.length && output.size() < contextsize; lno++ )
        {
            output.add(lines[lno]);
        }
        return output;
    }


    private static <T> T assertNotNull(T t)
    {
        if (t == null)
            throw new InternalExpressionException("Operand may not be null");
        return t;
    }

    private static <T> void assertNotNull(T t1, T t2)
    {
        if (t1 == null)
            throw new InternalExpressionException("First operand may not be null");
        if (t2 == null)
            throw new InternalExpressionException("Second operand may not be null");
    }

    static NumericValue getNumericValue(Value v1)
    {
        if (!(v1 instanceof NumericValue))
            throw new InternalExpressionException("Operand has to be of a numeric type");
        return ((NumericValue) v1);
    }

    private void addLazyUnaryOperator(String surface, int precedence, boolean leftAssoc,
                                       TriFunction<Context, Integer, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface+"u", new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer t, Expression e, Tokenizer.Token token, LazyValue v, LazyValue v2)
            {
                try
                {
                    if (v2 != null)
                    {
                        throw new ExpressionException(e, token, "Did not expect a second parameter for unary operator");
                    }
                    assertNotNull(v);
                    return lazyfun.apply(c, t, v);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, token, exc.getMessage());
                }
                catch (ArithmeticException exc)
                {
                    throw new ExpressionException(e, token, "Your math is wrong, "+exc.getMessage());
                }
            }
        });
    }


    private void addLazyBinaryOperatorWithDelegation(String surface, int precedence, boolean leftAssoc,
                                       SexFunction<Context, Integer, Expression, Tokenizer.Token, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2)
            {
                try
                {
                    assertNotNull(v1, v2);
                    return lazyfun.apply(c, type, e, t, v1, v2);
                }
                catch (InternalExpressionException exc) // might not actually throw it
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
                catch (ArithmeticException exc)
                {
                    throw new ExpressionException(e, t, "Your math is wrong, "+exc.getMessage());
                }
            }
        });
    }

    private void addLazyBinaryOperator(String surface, int precedence, boolean leftAssoc,
                                       QuadFunction<Context, Integer, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer t, Expression e, Tokenizer.Token token, LazyValue v1, LazyValue v2)
            {
                try
                {
                    assertNotNull(v1, v2);
                    return lazyfun.apply(c, t, v1, v2);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, token, exc.getMessage());
                }
                catch (ArithmeticException exc)
                {
                    throw new ExpressionException(e, token, "Your math is wrong, "+exc.getMessage());
                }
            }
        });
    }

    private void addUnaryOperator(String surface, boolean leftAssoc, Function<Value, Value> fun)
    {
        operators.put(surface+"u", new AbstractUnaryOperator(precedence.get("unary+-!"), leftAssoc)
        {
            @Override
            public Value evalUnary(Value v1)
            {
                return fun.apply(assertNotNull(v1));
            }
        });
    }

    private void addBinaryOperator(String surface, int precedence, boolean leftAssoc, BiFunction<Value, Value, Value> fun)
    {
        operators.put(surface, new AbstractOperator(precedence, leftAssoc)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return fun.apply(v1, v2);
            }
        });
    }


    private void addUnaryFunction(String name, Function<Value, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name,  new AbstractFunction(1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return fun.apply(assertNotNull(parameters.get(0)));
            }
        });
    }

    void addBinaryFunction(String name, BiFunction<Value, Value, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name, new AbstractFunction(2)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                Value v1 = parameters.get(0);
                Value v2 = parameters.get(1);
                assertNotNull(v1, v2);
                return fun.apply(v1, v2);
            }
        });
    }

    private void addFunction(String name, Function<List<Value>, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name, new AbstractFunction(-1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                for (Value v: parameters)
                    assertNotNull(v);
                return fun.apply(parameters);
            }
        });
    }

    private void addMathematicalUnaryFunction(String name, Function<Double, Double> fun)
    {
        addUnaryFunction(name, (v) -> new NumericValue(fun.apply(getNumericValue(v).getDouble())));
    }

    private void addMathematicalBinaryFunction(String name, BiFunction<Double, Double, Double> fun)
    {
        addBinaryFunction(name, (w, v) ->
                new NumericValue(fun.apply(getNumericValue(w).getDouble(), getNumericValue(v).getDouble())));
    }


    void addLazyFunction(String name, int num_params, TriFunction<Context, Integer, List<LazyValue>, LazyValue> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name, new AbstractLazyFunction(num_params)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer i, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
                try
                {
                    return fun.apply(c, i, lazyParams);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
                catch (ArithmeticException exc)
                {
                    throw new ExpressionException(e, t, "Your math is wrong, "+exc.getMessage());
                }
            }
        });
    }
    private void addContextFunction(String name, Expression expr, Tokenizer.Token token, List<String> arguments, List<String> globals, LazyValue code)
    {
        name = name.toLowerCase(Locale.ROOT);
        if (functions.containsKey(name))
            throw new ExpressionException(expr, token, "Function "+name+" would mask a built-in function");
        Expression function_context;
        try
        {
            function_context = expr.clone();
            function_context.name = name;
        }
        catch (CloneNotSupportedException e)
        {
            throw new ExpressionException(expr, token, "Problems in allocating global function "+name);
        }

        globalFunctions.put(name, new UserDefinedFunction(arguments, function_context, token)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
                if (arguments.size() != lazyParams.size()) // something that might be subject to change in the future
                {
                    throw new ExpressionException(e, t,
                            "Incorrect number of arguments for function "+name+
                            ". Should be "+arguments.size()+", not "+lazyParams.size()+" like "+arguments
                    );
                }
                Context newFrame = c.recreateFor(e);

                for (String global : globals)
                {
                    LazyValue  lv = c.getVariable(global);
                    if (lv == null)
                    {
                        newFrame.setVariable(global, (cc, tt) -> Value.ZERO.reboundedTo(global));
                    }
                    else
                    {
                        newFrame.setVariable(global, lv);
                    }
                }
                for (int i=0; i<arguments.size(); i++)
                {
                    String arg = arguments.get(i);
                    Value val = lazyParams.get(i).evalValue(c);
                    newFrame.setVariable(arg, (cc, tt) -> val.reboundedTo(arg)); // bindTo or reboundTo
                }
                Value retVal;
                boolean rethrow = false;
                try
                {
                    retVal = code.evalValue(newFrame, type); // todo not sure if we need to propagete type / consider boolean context in defined functions - answer seems ye
                }
                catch (ReturnStatement returnStatement)
                {
                    retVal = returnStatement.retval;
                }
                catch (ThrowStatement throwStatement)
                {
                    retVal = throwStatement.retval;
                    rethrow = true;
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(function_context, t, exc.getMessage());
                }
                catch (ArithmeticException exc)
                {
                    throw new ExpressionException(function_context, t, "Your math is wrong, "+exc.getMessage());
                }
                for (String global: globals)
                {
                    LazyValue lv = newFrame.getVariable(global);
                    if (lv != null)
                    {
                        c.setVariable(global, lv);
                    }
                }
                if (rethrow)
                {
                    throw new ThrowStatement(retVal);
                }
                Value otherRetVal = retVal;
                return (cc, tt) -> otherRetVal;
            }
        });
    }

    /**
     * <h1>Constants</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>Scarpet provides a number of constants that can be used literally in scripts</p>
     * <ul>
     *     <li><code>null</code>: nothing, zilch</li>
     * </ul>
     * <p>Other Paragraph</p>
     * </div>
     */

    public void Constants() // public just to get the Javadocs right
    {
        // all declared as global variables to save on switching scope cost
    }

    /**
     * <h1>User-defined functions and program control flow</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <h2>Writing programs with more than 1 line</h2>
     * <h3>Operator <code>;</code></h3>
     * <p>To effectively write programs that have more than one line, a programmer needs way to specify a sequence of
     * commands that execute one after another. In <code>scarpet</code> this can be achieved with <code>;</code>. Its an operator,
     * and by separating statements with semicolons. And since whitespaces and <code>$</code> signs are all treats as
     * whitespaces, how you layout your code doesn't matter, as long as it is readable to everyone involved. The
     * usefulness of preceding all the lines of the script with <code>$</code> is explained in the preamble</p>
     * <pre>
     * expr;
     * expr;
     * expr;
     * expr
     * </pre>
     * <p>Notice that the last expression is not followed by a semicolon. Since instruction separation is functional
     * in <code>scarpet</code>, and not barely an instruction delimiter,
     * terminating the code with a dangling operator wouldn't be valid.</p>
     * <p>In general <code>expr; expr; expr; expr</code> is equivalent to
     * <code>(((expr ; expr) ; expr) ; expr)</code>. In case the programmer forgets that it should be warned with a
     * helpful error at compile time.</p>
     * <p>Result of the evaluated expression is the same as the result of the second expression, but first expression is
     * also evaluated for sideeffects</p>
     * <pre>
     * expr1 ; expr2 =&gt; expr2  // with expr1 as a sideeffect
     * </pre>
     * <p>All defined functions are compiled, stored persistently, and available globally -
     * accessible to all other scripts. Functions can only be undefined via call to <code>undef('fun')</code>, which
     * would erase global entry for function <code>fun</code>. Since all variables have local scope inside each function,
     * one way to share large objects is via global variables
     * </p>
     * <h2>Global variables</h2>
     * <p>Any variable that is used with a name that starts with <code>'global_'</code> will be stored and accessible globally,
     * not, inside current scope. It will also persist across scripts, so if a procedure needs to use its own construct, it needs to
     * define it, or initialize it explicitly, or undefine it via <code>undef</code></p>
     * <pre>
     * a() -&gt; global_list+=1; global_list = l(1,2,3); a(); a(); global_list  // =&gt; [1,2,3,1,1]
     * </pre>
     * <h3>Operator <code>-&gt;</code></h3>
     * <p>To organize code better than a flat sequence of operations, one can define functions. Definition is correct if
     * has the following form</p>
     * <pre>
     *     fun(args, ...) -&gt; expr
     * </pre>
     * <p>Where <code>fun(args, ...)</code> is a function signature indicating function name, number of arguments,
     * and their names, and expr is an expression (can be complex) that is evaluated when <code>fun</code> is called.
     * Names in the signature don't need to be used anywhere else, other occurrences of these names
     * will be masked in this function scope.
     * Function call creates new scope for variables inside <code>expr</code>, so all non-global variables are not
     * visible from the caller scope. All parameters are passed by value to the new scope, including lists</p>
     * <pre>
     * a(lst) -&gt; lst+=1; list = l(1,2,3); a(list); a(list); list  // =&gt; [1,2,3]
     * </pre>
     * <p>In case the inner function wants to operate and modify larger objects, lists from the outer
     * scope, but not global, it needs to use <code>outer</code> function in function signature</p>
     * <h3><code>outer(arg)</code> function</h3>
     * <p><code>outer</code> function can only be used in the function signature, and it will
     * cause an error everywhere else. It borrows the reference to that variable from the outer scope and allows
     * its modification in the inner scope. Any modification of outer variable will result in change of them in
     * the outer function. In case the variable was not set yet in the outer scope - it will be created. This construct
     * is similar to <code>nonlocal</code> scoping from python</p>
     * <pre>
     * a(outer(list)) -&gt; list+=1; list = l(1,2,3); a(); a(); list  // =&gt; [1,2,3,1,1]
     * </pre>
     * <p>The return value of a function is the value of the last expression. This as the same effect as using outer
     * or global lists, but is more expensive</p>
     * <pre>
     * a(lst) -&gt; lst+=1; list = l(1,2,3); list=a(list); list=a(list); list  // =&gt; [1,2,3,1,1]
     * </pre>
     * <p>Ability to combine more statements into one expression, with functions, passing parameters, and global and outer
     * scoping allow to organize even larger scripts</p>
     * <h2>Control flow</h2>
     * <h3><code>return(expr)</code></h3>
     * <p>Sometimes its convenient to break the organized control flow, or it is not practical to pass
     * the final result value of a function to the last statement, in this case a return statement can be used</p>
     * <pre>
     * def() -&gt; (
     *  expr1;
     *  expr2;
     *  return(expr3); // function terminates returning expr3
     *  expr4;     // skipped
     *  expr5      // skipped
     * )
     * </pre>
     * <p>In general its cheaper to leave the last expression as a return value, rather than calling returns everywhere,
     * but it would often lead to a messy code.</p>
     * <h3><code>exit(expr)</code></h3>
     * <p>It terminates entire program passing <code>expr</code> as the result of the program execution.</p>
     * <h3><code>try(expr, catch_expr(_)) ... throw(value)</code></h3>
     * <p><code>try</code> function evaluates expression, and continues further unless <code>throw</code> function is called
     * anywhere inside <code>expr</code>. In that case the <code>catch_expr</code> is evaluates with <code>_</code> set
     * to the argument <code>throw</code> was called with. This mechanism allows to terminate large portion of a convoluted
     * call stack and continue program execution. There is only one level of exceptions currently in carpet, so if the inner
     * function also defines the <code>try</code> catchment area, it will received the exception first, but it can technically
     * rethrow the value its getting for the outer scope. Unhandled throw acts like an exit statement.</p>
     * <h3>if(cond, expr, cond?, expr?, ..., default?) </h3>
     * <p>If statement is a function that takes a number of conditions that are evaluated one after another and if
     * any of them turns out true, its <code>expr</code> gets returned, otherwise, if all conditions fail, the return value is
     * <code>default</code> expression, or <code>null</code> if default is skipped</p>
     * <p><code>if</code> function is equivalent to <code>if (cond) expr; else if (cond) expr; else default;</code>
     * from Java, just in a functional form </p>
     * </div>
     */

    public void UserDefinedFunctionsAndControlFlow() // public just to get the javadoc right
    {
        // artificial construct to handle user defined functions and function definitions
        addLazyFunction(".",-1, (c, t, lv) -> { // adjust based on c
            String name = lv.get(lv.size()-1).evalValue(c).getString();
            //lv.remove(lv.size()-1); // aint gonna cut it
            if (t != Context.SIGNATURE) // just call the function
            {
                if (!globalFunctions.containsKey(name))
                {
                    throw new InternalExpressionException("Function "+name+" is not defined yet");
                }
                List<LazyValue> lvargs = new ArrayList<>(lv.size()-1);
                for (int i=0; i< lv.size()-1; i++)
                {
                    lvargs.add(lv.get(i));
                }
                UserDefinedFunction acf = globalFunctions.get(name);
                return (cc, tt) -> acf.lazyEval(c, t, acf.expression, acf.token, lvargs).evalValue(c); ///!!!! dono might need to store expr and token in statics? (e? t?)
            }

            // gimme signature
            List<String> args = new ArrayList<>();
            List<String> globals = new ArrayList<>();
            for (int i = 0; i < lv.size() - 1; i++)
            {
                Value v = lv.get(i).evalValue(c, Context.LOCALIZATION);
                if (!v.isBound())
                {
                    throw new InternalExpressionException("Only variables can be used in function signature, not  " + v.getString());
                }
                if (v instanceof GlobalValue)
                {
                    globals.add(v.boundVariable);
                }
                else
                {
                    args.add(v.boundVariable);
                }
            }
            return (cc, tt) -> new FunctionSignatureValue(name, args, globals);
        });
        addLazyFunction("outer", 1, (c, t, lv) -> {
            if (t != Context.LOCALIZATION)
                throw new InternalExpressionException("outer scoping of variables is only possible in function signatures");
            return (cc, tt) -> new GlobalValue(lv.get(0).evalValue(c));
        });

        addLazyBinaryOperator(";",precedence.get("nextop;"), true, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.VOID);
            if (c.getLogger() != null)
                c.getLogger().accept(v1.getString());
            return lv2;
        });

        //assigns const procedure to the lhs, returning its previous value
        addLazyBinaryOperatorWithDelegation("->", precedence.get("def->"), false, (c, type, e, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.SIGNATURE);
            if (v1 instanceof FunctionSignatureValue)
            {
                FunctionSignatureValue sign = (FunctionSignatureValue) v1;
                addContextFunction(sign.getName(), e, t, sign.getArgs(), sign.getGlobals(), lv2);
            }
            else
            {
                v1.assertAssignable();
                c.setVariable(v1.getVariable(), lv2);
            }
            return (cc, tt) -> new StringValue("OK");
        });

        addUnaryFunction("exit", (v) -> { throw new ExitStatement(v); });
        addUnaryFunction("return", (v) -> { throw new ReturnStatement(v); });
        addUnaryFunction("throw", (v)-> {throw new ThrowStatement(v); });

        addLazyFunction("try", 2, (c, t, lv) -> (c_, t_) ->
        {
            try
            {
                return lv.get(0).evalValue(c_, t_);
            }
            catch (ThrowStatement ret)
            {
                LazyValue __ = c_.getVariable("_");
                c_.setVariable("_", (__c, __t) -> ret.retval.bindTo("_"));
                Value val = lv.get(1).evalValue(c_, t_);
                c_.setVariable("_",__);
                return val;
            }
        });

        // give it a thought why this doesn't work
        // script run g(outer(a)) -> ( if(a>2,throw(a),a+5) ); f(a) -> ( try2(a+=1; g(); a+= 1, a+=10*a); a ); print(f(1)); print(f(2)); print(f(3))
        // and it works with 'try'
        addLazyFunction("try2", 2, (c, t, lv) ->
        {
            try
            {
                return (c_, t_) -> lv.get(0).evalValue(c, t);
            }
            catch (ThrowStatement ret)
            {
                LazyValue __ = c.getVariable("_");
                c.setVariable("_", (__c, __t) -> ret.retval.bindTo("_"));
                Value val = lv.get(1).evalValue(c, t);
                c.setVariable("_",__);
                return (c_, t_) -> val;
            }
        });

        // if(cond1, expr1, cond2, expr2, ..., ?default) => value
        addLazyFunction("if", -1, (c, t, lv) ->
        {
            if ( lv.size() < 2 )
                throw new InternalExpressionException("if statement needs to have at least one condition and one case");
            for (int i=0; i<lv.size()-1; i+=2)
            {
                if (lv.get(i).evalValue(c, Context.BOOLEAN).getBoolean())
                {
                    int iFinal = i;
                    return (cc, tt) -> lv.get(iFinal+1).evalValue(c);
                }
            }
            if (lv.size()%2 == 1)
                return (cc, tt) -> lv.get(lv.size() - 1).evalValue(c);
            return (cc, tt) -> new NumericValue(0);
        });
    }

    /**
     * <h1>Operators</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     *
     * <p>There is a number of operators you can use inside the expressions. Those could be considered
     * generic type operators that apply to most data types. They also follow standard operator
     * precedence, i.e. <code>2+2*2</code> is understood as <code>2+(2*2)</code>,
     * not <code>(2+2)*2</code>, otherwise they are applied from left to right, i.e.
     * <code>2+4-3</code> is interpreted as <code>(2+4)-3</code>, which in case of numbers
     * doesn't matter, but since <code>scarpet</code> allows for mixing all value types
     * the associativity would matter, and may lead to unintended effects: </p>
     * <p>Important operator is function definition <code>-&gt;</code> operator. It will be covered
     * in {@link carpet.script.Expression#UserDefinedFunctionsAndControlFlow}</p>
     * <pre>
     * '123'+4-2 =&gt; ('123'+4)-2 =&gt; '1234'-2 =&gt; '134'
     * '123'+(4-2) =&gt; '123'+2 =&gt; '1232'
     * 3*'foo' =&gt; 'foofoofoo'
     * 1357-5 =&gt; 1352
     * 1357-'5' =&gt; 137
     * 3*'foo'-'o' =&gt; 'fff'
     * l(1,3,5)+7 =&gt; l(8,10,12)
     * </pre>
     * <p>As you can see, values can behave differently when mixed with other types
     * in the same expression. in case values are of the same types, the result
     * tends to be obvious, but <code>Scarpet</code> tries to make sense of whatever it
     * has to deal with</p>
     * <h2>Operator Precedence</h2>
     * <p>
     *     Here is the complete list of operators in <code>scarpet</code> including control flow operators.
     *     note, that commas and brackets are not operators, but behave like them:
     *     </p>
     *     <ul>
     *         <li>Unary <code>+ - !</code></li>
     *         <li>Exponent <code>^</code></li>
     *         <li>Multiplication <code>* / %</code></li>
     *         <li>Addition <code>+ -</code></li>
     *         <li>Comparison <code>== != &gt; &gt;= &lt;= &lt; ~</code></li>
     *         <li>Logical And<code>&amp;&amp;</code></li>
     *         <li>Logical Or <code>||</code></li>
     *         <li>Assignment <code>= =+ &lt;&gt;</code></li>
     *         <li>Definition <code>-&gt;</code></li>
     *         <li>Next statement<code>;</code></li>
     *         <li>Comma <code>,</code></li>
     *         <li>Bracket <code>( )</code></li>
     *     </ul>
     *
     * <h3>Operator '+', '-', '*', '/'</h3>
     * <p>Allows to add the results of two expressions. If the operands resolve to numbers, the result is
     * arithmetic operation.
     * In case of strings, adding or subtracting from a string results in string concatenation and
     * removal os substrings from that string. Multiplication of strings and numbers results in repeating the
     * string N times and division results in taking the first k'th part of the string, so that <code>str*n/n ~ str</code>
     * In case first operand is a list, either it results in a new list
     * with all elements modified one by one with the other operand, or if the operand is a list with the same number of
     * items - elementwise addition/subtraction</p>
     * <p>Examples:</p>
     * <pre>
     * 2+3 =&gt; 5
     * 'foo'+3+2 =&gt; 'foo32'
     * 'foo'+(3+2) =&gt; 'foo5'
     * 3+2+'bar' =&gt; '5bar'
     * 'foo'*3 =&gt; 'foofoofoo'
     * 'foofoofoo' / 3 =&gt; 'foo'
     * 'foofoofoo'-'o' =&gt; 'fff'
     * l(1,2,3)+1  =&gt; l(2,3,4)
     * b = l(100,63,100); b+l(10,0,10)  =&gt; l(110,63,110)
     * </pre>
     *
     * <h3>Operator '%', '^'</h3>
     * <p>The modulo and exponent (power) operators work only if both operands are numbers</p>
     * <pre>
     * pi^pi%euler  =&gt; 1.124....
     * -9 % 4  =&gt; -1
     * 9 % -4  =&gt; 0 ¯\_(ツ)_/¯ Java
     * -3 ^ 2  =&gt; 9
     * -3 ^ pi =&gt; // Error
     * </pre>
     *
     * <h3>Operator '==', '!=', '&lt;', '&gt;', '&lt;=', '&gt;=' </h3>
     * <p>Allows to compare the results of two expressions.
     * For numbers it is considers arithmetic order of numbers, for strings - lexicographical,
     * nulls are always 'less' than everything else, and lists check their elements - if the sizes
     * are different, the size matters, otherwise, pairwise comparisons for each elements are performed.
     * The same order rules than with all these operators are used with the default sortographical order as
     * used by <code>sort</code> function. All of these are true:
     * </p>
     * <pre>
     * null == null
     * null != false
     * 0 == false
     * 1 == true
     * null &lt; 0
     * null &lt; -1000
     * 1000 &lt; 'a'
     * 'bar' &lt; 'foo'
     * 3 == 3.0
     * </pre>
     *
     * <h3>Operator '&amp;&amp;', '||'</h3>
     * <p>These operator compute respective boolean operation on the operands. What it important is that if calculating
     * of the second operand is not necessary, it won't be evaluated, which means one can use them as conditional
     * statements</p>
     * <pre>
     * true || false  =&gt; 1
     * null || false =&gt; 0
     * null != false || run('kill gnembon')  =&gt; 1 // gnembon survives
     * null != false &amp;&amp; run('kill gnembon')  =&gt; 0 // when cheats not allowed
     * null != false &amp;&amp; run('kill gnembon')  =&gt; 1 // gnembon dies, cheats allowed
     * </pre>
     *
     * <h3>Operator ~ </h3>
     * <p>This operator should be understood as 'matches', or 'in'. For strings it matches the right operand as a regular
     * expression to the left one, returning the first match. This can be used to extract information from unparsed nbt's
     * in a more efficient way. For lists it checks if an element is in the list, and returns the index of that element,
     * or <code>null</code> if no such element was found, especially that the use of <code>first</code> function will not
     * return the index. Currently it doesn't have any special behaviour for numbers - it checks for existence of characters
     * in string representation of the left operand with respect of the regular expression on the right hand side.
     * string</p>
     * <pre>
     * l(1,2,3) ~ 2  =&gt; 1
     * l(1,2,3) ~ 4  =&gt; null
     * 'foobar' ~ '.b'  =&gt; 'ob'
     * players('*') ~ 'gnembon'  // null unless player gnembon is logged in (better to use player('gnembon') instead
     * </pre>
     * <p>Or a longer example of an ineffective way to searching for a squid</p>
     * <pre>
     * entities = entities_area('all',x,y,z,100,10,100);
     * sid = entities ~ 'Squid';
     * if(sid != null, run('execute as '+query(element(entities,sid),'id')+' run say I am here '+query(element(entities,sid),'pos') ) )
     * </pre>
     * <p>Or an example to find if a player has specific enchantment on a held axe (either hand) and get its level
     * (despite obvious lack of support for json NBT's):</p>
     * <pre>
     * global_get_enchantment(p, ench) -&gt; (
     * $   for(l('main','offhand'),
     * $      holds = query(p, 'holds', _);
     * $      if( holds,
     * $         l(what, count, nbt) = holds;
     * $         if( what ~ '_axe' &amp;&amp; nbt ~ ench,
     * $            lvl = max(lvl, number(nbt ~ '(?&lt;=lvl:)\\d') )
     * $         )
     * $      )
     * $   );
     * $   lvl
     * $);
     * /script run global_get_enchantment(players(), 'sharpness')
     * </pre>
     * <h3>Operator =, &lt;&gt;, +=</h3>
     * <p>A set of assignment operators. All require bounded variable on the LHS, <code>&lt;&gt;</code> requires
     * bounded arguments on the right hand side as well (bounded, meaning being variables). Additionally they can also
     * handle list constructors with all bounded variables, and work then as list assignment operators.
     * When <code>+=</code> is used on a list, it extends that list of that element, and returns the list (old == new).
     * <code>scarpet</code> doesn't support currently removal of items. Removal of items can be obtaine via
     * <code>filter</code> command, and reassigning it fo the same variable. Both operations would require rewriting of the
     * array anyways.</p>
     * <pre>
     * a = 5  =&gt; a == 5
     * l(a,b,c) = l(3,4,5) =&gt; a==3, b==4, c==5
     * l(minx,maxx) = sort(xi,xj);  // minx assumes min(xi, xj) and maxx, max(xi, xj)
     * l(a,b,c,d,e,f) = l(range(6)); l(a,b,c) &lt;&gt; l(d,e,f); l(a,b,c,d,e,f)  =&gt; [3,4,5,0,1,2]
     * a = l(1,2,3); a += 4  =&gt; [1,2,3,4]
     * a = l(1,2,3,4); a = filter(a,_!=2)  =&gt; [1,3,4]
     * </pre>
     * <h3>Unary Operator -, +</h3>
     * <p>Require a number, flips the sign. One way to assert its a number by crashing the script. gg.</p>
     * <pre>
     * -4  =&gt; -4
     * +4  =&gt; 4
     * +'4'  // Error message
     * </pre>
     *
     * <h3>Operator !</h3>
     * <p>flops boolean condition of the expression. Equivalent of <code>bool(expr)==false</code></p>
     * <pre>
     * !true  =&gt; 0
     * !false  =&gt; 1
     * !null  =&gt; 1
     * !5  =&gt; 0
     * !l() =&gt; 1
     * !l(null) =&gt; 0
     * </pre>
     * </div>
     */
    public void Operators()
    {
        addBinaryOperator("+", precedence.get("addition+-"), true, Value::add);
        addBinaryOperator("-", precedence.get("addition+-"), true, Value::subtract);
        addBinaryOperator("*", precedence.get("multiplication*/%"), true, Value::multiply);
        addBinaryOperator("/", precedence.get("multiplication*/%"), true, Value::divide);
        addBinaryOperator("%", precedence.get("multiplication*/%"), true, (v1, v2) ->
                new NumericValue(getNumericValue(v1).getDouble() % getNumericValue(v2).getDouble()));
        addBinaryOperator("^", precedence.get("exponent^"), false, (v1, v2) ->
                new NumericValue(Math.pow(getNumericValue(v1).getDouble(), getNumericValue(v2).getDouble())));

        addLazyBinaryOperator("&&", precedence.get("and&&"), false, (c, t, lv1, lv2) ->
        {
            boolean b1 = lv1.evalValue(c, Context.BOOLEAN).getBoolean();
            if (!b1) return LazyValue.FALSE;
            boolean b2 = lv2.evalValue(c, Context.BOOLEAN).getBoolean();
            return b2 ? LazyValue.TRUE : LazyValue.FALSE;
        });

        addLazyBinaryOperator("||", precedence.get("or||"), false, (c, t, lv1, lv2) ->
        {
            boolean b1 = lv1.evalValue(c, Context.BOOLEAN).getBoolean();
            if (b1) return LazyValue.TRUE;
            boolean b2 = lv2.evalValue(c, Context.BOOLEAN).getBoolean();
            return b2 ? LazyValue.TRUE : LazyValue.FALSE;
        });

        addBinaryOperator("~", precedence.get("compare>=><=<"), true, Value::in);

        addBinaryOperator(">", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) > 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator(">=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) >= 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("<", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) < 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("<=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) <= 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("==", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.TRUE : Value.FALSE);
        addBinaryOperator("!=", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.FALSE : Value.TRUE);

        addLazyBinaryOperator("=", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c);
            Value v2 = lv2.evalValue(c);
            if (v1 instanceof ListValue.ListConstructorValue && v2 instanceof ListValue)
            {
                List<Value> ll = ((ListValue)v1).getItems();
                List<Value> rl = ((ListValue)v2).getItems();
                if (ll.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                if (ll.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                for (Value v: ll) v.assertAssignable();
                Iterator<Value> li = ll.iterator();
                Iterator<Value> ri = rl.iterator();
                while(li.hasNext())
                {
                    String lname = li.next().getVariable();
                    Value vval = ri.next();
                    c.setVariable(lname, (cc, tt) -> vval.reboundedTo(lname));
                }
                return (cc, tt) -> Value.TRUE;
            }
            v1.assertAssignable();
            String varname = v1.getVariable();
            LazyValue boundedLHS = (cc, tt) -> v2.reboundedTo(varname);
            c.setVariable(varname, boundedLHS);
            return boundedLHS;
        });

        addLazyBinaryOperator("+=", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c);
            Value v2 = lv2.evalValue(c);
            if (v1 instanceof ListValue.ListConstructorValue && v2 instanceof ListValue)
            {
                List<Value> ll = ((ListValue)v1).getItems();
                List<Value> rl = ((ListValue)v2).getItems();
                if (ll.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                if (ll.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                for (Value v: ll) v.assertAssignable();
                Iterator<Value> li = ll.iterator();
                Iterator<Value> ri = rl.iterator();
                while(li.hasNext())
                {
                    Value lval = li.next();
                    String lname = lval.getVariable();
                    Value vval = ri.next();
                    c.setVariable(lname, (cc, tt) -> lval.add(vval).bindTo(lname));
                }
                return (cc, tt) -> Value.TRUE;
            }
            v1.assertAssignable();
            String varname = v1.getVariable();
            LazyValue boundedLHS;
            if (v1 instanceof ListValue)
            {
                ((ListValue) v1).append(v2);
                boundedLHS = (cc, tt)-> v1;
            }
            else
            {
                boundedLHS = (cc, tt) -> v1.add(v2).bindTo(varname);
            }
            c.setVariable(varname, boundedLHS);
            return boundedLHS;
        });

        addLazyBinaryOperator("<>", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c);
            Value v2 = lv2.evalValue(c);
            if (v1 instanceof ListValue.ListConstructorValue && v2 instanceof ListValue.ListConstructorValue)
            {
                List<Value> ll = ((ListValue)v1).getItems();
                List<Value> rl = ((ListValue)v2).getItems();
                if (ll.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                if (ll.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                for (Value v: ll) v.assertAssignable();
                for (Value v: rl) v.assertAssignable();
                Iterator<Value> li = ll.iterator();
                Iterator<Value> ri = rl.iterator();
                while(li.hasNext())
                {
                    Value lval = li.next();
                    Value rval = ri.next();
                    String lname = lval.getVariable();
                    String rname = rval.getVariable();
                    c.setVariable(lname, (cc, tt) -> rval.reboundedTo(lname));
                    c.setVariable(rname, (cc, tt) -> lval.reboundedTo(rname));
                }
                return (cc, tt) -> Value.TRUE;
            }
            v1.assertAssignable();
            v2.assertAssignable();
            String lvalvar = v1.getVariable();
            String rvalvar = v2.getVariable();
            Value lval = v2.reboundedTo(lvalvar);
            Value rval = v1.reboundedTo(rvalvar);
            c.setVariable(lvalvar, (cc, tt) -> lval);
            c.setVariable(rvalvar, (cc, tt) -> rval);
            return (cc, tt) -> lval;
        });

        addUnaryOperator("-",  false, (v) -> new NumericValue(-getNumericValue(v).getDouble()));

        addUnaryOperator("+", false, (v) -> new NumericValue(getNumericValue(v).getDouble()));

        addLazyUnaryOperator("!", precedence.get("unary+-!"), false, (c, t, lv)-> lv.evalValue(c, Context.BOOLEAN).getBoolean() ? (cc, tt)-> Value.FALSE : (cc, tt) -> Value.TRUE); // might need context boolean

    }

    /**
     * <h1>Arithmetic operations</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>Section Content</p>
     * <p>Other Paragraph</p>
     * </div>
     */
    public void ArithmeticOperations()
    {
        addLazyFunction("not", 1, (c, t, lv) -> lv.get(0).evalValue(c, Context.BOOLEAN).getBoolean() ? ((cc, tt) -> Value.FALSE) : ((cc, tt) -> Value.TRUE));

        addUnaryFunction("fact", (v) ->
        {
            long number = getNumericValue(v).getLong();
            long factorial = 1;
            for (int i = 1; i <= number; i++)
            {
                factorial = factorial * i;
            }
            return new NumericValue(factorial);
        });


        addMathematicalUnaryFunction("sin",    (d) -> Math.sin(Math.toRadians(d)));
        addMathematicalUnaryFunction("cos",    (d) -> Math.cos(Math.toRadians(d)));
        addMathematicalUnaryFunction("tan",    (d) -> Math.tan(Math.toRadians(d)));
        addMathematicalUnaryFunction("asin",   (d) -> Math.toDegrees(Math.asin(d)));
        addMathematicalUnaryFunction("acos",   (d) -> Math.toDegrees(Math.acos(d)));
        addMathematicalUnaryFunction("atan",   (d) -> Math.toDegrees(Math.atan(d)));
        addMathematicalBinaryFunction("atan2", (d, d2) -> Math.toDegrees(Math.atan2(d, d2)) );
        addMathematicalUnaryFunction("sinh",   Math::sinh );
        addMathematicalUnaryFunction("cosh",   Math::cosh  );
        addMathematicalUnaryFunction("tanh",   Math::tanh );
        addMathematicalUnaryFunction("sec",    (d) ->  1.0 / Math.cos(Math.toRadians(d)) ); // Formula: sec(x) = 1 / cos(x)
        addMathematicalUnaryFunction("csc",    (d) ->  1.0 / Math.sin(Math.toRadians(d)) ); // Formula: csc(x) = 1 / sin(x)
        addMathematicalUnaryFunction("sech",   (d) ->  1.0 / Math.cosh(d) );                // Formula: sech(x) = 1 / cosh(x)
        addMathematicalUnaryFunction("csch",   (d) -> 1.0 / Math.sinh(d)  );                // Formula: csch(x) = 1 / sinh(x)
        addMathematicalUnaryFunction("cot",    (d) -> 1.0 / Math.tan(Math.toRadians(d))  ); // Formula: cot(x) = cos(x) / sin(x) = 1 / tan(x)
        addMathematicalUnaryFunction("acot",   (d) ->  Math.toDegrees(Math.atan(1.0 / d)) );// Formula: acot(x) = atan(1/x)
        addMathematicalUnaryFunction("coth",   (d) ->  1.0 / Math.tanh(d) );                // Formula: coth(x) = 1 / tanh(x)
        addMathematicalUnaryFunction("asinh",  (d) ->  Math.log(d + (Math.sqrt(Math.pow(d, 2) + 1))));  // Formula: asinh(x) = ln(x + sqrt(x^2 + 1))
        addMathematicalUnaryFunction("acosh",  (d) ->  Math.log(d + (Math.sqrt(Math.pow(d, 2) - 1))));  // Formula: acosh(x) = ln(x + sqrt(x^2 - 1))
        addMathematicalUnaryFunction("atanh",  (d) ->                                       // Formula: atanh(x) = 0.5*ln((1 + x)/(1 - x))
        {
            if (Math.abs(d) > 1 || Math.abs(d) == 1)
                throw new InternalExpressionException("Number must be |x| < 1");
            return 0.5 * Math.log((1 + d) / (1 - d));
        });
        addMathematicalUnaryFunction("rad",  Math::toRadians);
        addMathematicalUnaryFunction("deg", Math::toDegrees);
        addMathematicalUnaryFunction("ln", Math::log);
        addMathematicalUnaryFunction("ln1p", Math::log1p);
        addMathematicalUnaryFunction("log10", Math::log10);
        addMathematicalUnaryFunction("log", a -> Math.log(a)/Math.log(2));
        addMathematicalUnaryFunction("log1p", x -> Math.log1p(x)/Math.log(2));

        addMathematicalUnaryFunction("sqrt", Math::sqrt);
        addMathematicalUnaryFunction("abs", Math::abs);
        addMathematicalUnaryFunction("round", (d) -> (double)Math.round(d));
        addMathematicalUnaryFunction("floor", Math::floor);
        addMathematicalUnaryFunction("ceil", Math::ceil);

        addLazyFunction("mandelbrot", 3, (c, t, lv) -> {
            double a0 = getNumericValue(lv.get(0).evalValue(c)).getDouble();
            double b0 = getNumericValue(lv.get(1).evalValue(c)).getDouble();
            long maxiter = getNumericValue(lv.get(2).evalValue(c)).getLong();
            double a = 0.0D;
            double b = 0.0D;
            long iter = 0;
            while(a*a+b*b<4 && iter < maxiter)
            {
                double temp = a*a-b*b+a0;
                b = 2*a*b+b0;
                a = temp;
                iter++;
            }
            long iFinal = iter;
            return (cc, tt) -> new NumericValue(iFinal);
        });

        addFunction("max", (lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("MAX requires at least one parameter");
            Value max = null;
            for (Value parameter : lv)
            {
                if (max == null || parameter.compareTo(max) > 0) max = parameter;
            }
            return max;
        });

        addFunction("min", (lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("MIN requires at least one parameter");
            Value min = null;
            for (Value parameter : lv)
            {
                if (min == null || parameter.compareTo(min) < 0) min = parameter;
            }
            return min;
        });

        addUnaryFunction("relu", (v) -> v.compareTo(Value.ZERO) < 0 ? Value.ZERO : v);

    }



    /**
     * <h1>Lists, loops, and higher order functions</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <p>Efficient use of these functions cangreatly simplify your programs and speed them up, as these functions
     * will internalize  most of the operations that need to be applied on multiple values at the same time</p>
     * <h2>Basic list operations</h2>
     * <h3><code>l(values ...), l(iterator) </code></h3>
     * <p>Creates a list of values of the expressions passed as parameters. It can be used as an L-value and if all
     * elements are variables, you coujld use it to return multiple results from one function call, if that function returns
     * a list of results with the same size as the <code>l</code> call uses. In case there is only one argument and it is
     * an iterator (vanilla expression specification has <code>range</code>, but Minecraft API implements
     * a bunch of them, like <code>diamond</code>), it will convert it to a proper list. Iterators can only be used in
     * high order functions, and are treated as empty lists, unless unrolled with <code>l</code></p>
     * <pre>
     * l(1,2,'foo') =&gt; [1, 2, foo]
     * l() =&gt; [] (empty list)
     * l(range(10)) =&gt; [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
     * l(1, 2) = l(3, 4) =&gt; Error: l is not a variable
     * l(foo, bar) = l(3,4); foo==3 &amp;&amp; bar==4 =&gt; 1
     * l(foo, bar, baz) = l(2, 4, 6); l(min(foo, bar), baz) = l(3, 5); l(foo, bar, baz)  =&gt; [3, 4, 5]
     * </pre>
     * <p>In the last example <code>l(min(foo, bar), baz)</code> creates a valid L-value, as min(foo, bar) finds the
     * lower of the variables (in this case <code>foo</code>) creating a valid assignable L-list of [foo, baz], and these
     * values will be assigned new values</p>
     *
     * <h3><code>join(delim, list), join(delim, values ...) </code></h3>
     * <p>Returns a string that contains joined elements of the list, iterator, or all values, concatenated with <code>delim</code> delimiter</p>
     * <pre>
     *     join('-',range(10))  =&gt; 0-1-2-3-4-5-6-7-8-9
     *     join('-','foo')  =&gt; foo
     *     join('-', 'foo', 'bar')  =&gt; foo-bar
     * </pre>
     *
     * <h3><code>split(delim, expr)</code></h3>
     * <p>Splits a string undr <code>expr</code> by <code>delim</code> which can be a regular expression</p>
     * <pre>
     *     split('',foo)  =&gt; [f, o, o]
     *     split('.','foo.bar')  =&gt; []
     *     split('\\.','foo.bar')  =&gt; [foo, bar]
     * </pre>
     *
     * <h3><code>slice(expr, from, to?)</code></h3>
     * <p>extracts a substring, or sublist (based on the type of the result of the expression under expr with starting index
     * of <code>from</code>, and ending at <code>to</code> if provided, or the end, if omitted</p>
     * <pre>
     *     split('',foo)  =&gt; [f, o, o]
     *     split('.','foo.bar')  =&gt; []
     *     split('\\.','foo.bar')  =&gt; [foo, bar]
     * </pre>
     *
     *
     *
     * <h3><code>sort(list), sort(values ...) </code></h3>
     * <p>Sorts in the default sortographical order either all arguments, or a list if its the only argument. It returns a new
     * sorted list, not affecting the list passed to the argument</p>
     * <pre>
     * sort(3,2,1)  =&gt; [1, 2, 3]
     * sort('a',3,11,1)  =&gt; [1, 3, 11, 'a']
     * list = l(4,3,2,1); sort(list)  =&gt; [1, 2, 3, 4]
     * </pre>
     *
     * <h3><code>sort_key(list, key_expr)</code></h3>
     * <p>Sorts a copy of the list in the order or keys as defined by the <code>key_expr</code> for each element</p>
     * <pre>
     *     sort_key([1,3,2],_)  =&gt; [1, 2, 3]
     *     sort_key([1,3,2],-_)  =&gt; [3, 2, 1]
     *     sort_key(l(range(10)),rand(1))  =&gt; [1, 0, 9, 6, 8, 2, 4, 5, 7, 3]
     *     sort_key(l(range(20)),str(_))  =&gt; [0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9]
     * </pre>
     *
     * <h3><code>range(to), range(from, to), range(from, to, step)</code></h3>
     * <p>Creates a range of numbers from <code>from</code>, no greater/larger than <code>limit</code>.
     * The <code>step</code> parameter dictates not only the increment size, but also direction (can be negative).
     * The returned value is not a proper list, just the iterator
     * but if for whatever reason you need a proper list with all items evaluated, use <code>l(range(limit))</code>.
     * Primarily to be used in higher order functions</p>
     * <pre>
     *     range(10)  =&gt; [...]
     *     l(range(10))  =&gt; [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
     *     map(range(10),_*_)  =&gt; [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
     *     reduce(range(10),_a+_, 0)  =&gt; 45
     *     range(5,10)  =&gt; [5, 6, 7, 8, 9]
     *     range(20, 10, -2)  =&gt; [20, 18, 16, 14, 12]
     * </pre>
     *
     * <h3><code>element(list, index)</code></h3>
     * <p>Returns the value at <code>index</code> element from the <code>list</code>.
     * use negative numbers to reach elements from the end of the list. <code>element</code>
     * call will always be able to find the index. In case there is few items, it will loop over </p>
     * <pre>
     *     element(l(range(10)), 5)  =&gt; 5
     *     element(l(range(10)), -1)  =&gt; 9
     *     element(l(range(10)), 10)  =&gt; 0
     *     element(l(range(10)), 93)  =&gt; 3
     * </pre>
     *
     * <h3><code>while(cond, limit, expr)</code></h3>
     * <p>Evaluates expression <code>expr</code> repeatedly until condition <code>cond</code> becomes false,
     * but not more than <code>limit</code> times. Returns the result of the last <code>expr</code> evaluation,
     * or <code>null</code> if nothing was successful. both <code>expr</code> and <code>cond</code> will recveived a
     * bound variable <code>_</code> indicating current iteration, so its a number</p>
     * <pre>
     *  while(a&lt;100,10,a=_*_)  =&gt; 81 // loop exhausted via limit
     *  while(a&lt;100,20,a=_*_)  =&gt; 100 // loop stopped at condition, but a has already been assigned
     *  while(_*_&lt;100,20,a=_*_)  =&gt; 81 // loop stopped at condition, before a was assigned a value
     * </pre>
     *
     * <h3><code>loop(num,expr(_),exit(_)?)</code></h3>
     * <p>Evaluates expression <code>expr</code>, <code>num</code> number of times. Optionally,
     * if <code>cond</code> condition is present, stops the execution if the condition becomes true.
     * Both <code>expr</code> and <code>cond</code> receive <code>_</code> system variable indicating the iteration</p>
     * <pre>
     *     loop(5, tick())  =&gt; repeat tick 5 times
     *     list = l(); loop(5, x = _; loop(5, list += l(x, _) ) ); list
     *       // double loop, produces: [[0, 0], [0, 1], [0, 2], [0, 3], [0, 4], [1, 0], [1, 1], ... , [4, 2], [4, 3], [4, 4]]
     * </pre>
     * <p>In this small example we will search for first 10 primes, apparently including 0:</p>
     * <pre>
     *     check_prime(n) -&gt; !first( range(2, sqrt(n)+1), !(n % _) );
     *     primes = l();
     *     loop(10000, if(check_prime(_), primes += _), length(primes) &gt;= 10 );
     *     primes
     *
     *     // outputs: [0, 1, 2, 3, 5, 7, 11, 13, 17, 19]
     * </pre>
     * <h3><code>map(list,expr(_,_i), exit(_,_i)?)</code></h3>
     * <p>Converts a <code>list</code> of values, to another list where each value is result of an expression
     * <code>v = expr(_, _i)</code> where <code>_</code> is passed as each element of the list, and <code>_i</code> is
     * the index of such element, optional <code>exit</code> condition is evaluated after each mapping and if it is false,
     * the map is terminated and returned whatever got collected so far</p>
     * <pre>
     *     map(range(10), _*_)  =&gt; [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
     *     map(players('*'), _+' is stupid') [gnembon is stupid, herobrine is stupid]
     * </pre>
     *
     * <h3><code>filter(list,expr(_,_i),exit(_,_i))</code></h3>
     * <p>filters <code>list</code> elements returning only these that return positive result of the <code>expr</code>.
     * Like with <code>map</code> if the optional condition <code>exit</code> gets true, the list is returned up to this point</p>
     * <pre>
     *     filter(range(100), !(_%5), _*_&gt;1000)  =&gt; [0, 5, 10, 15, 20, 25, 30]
     *     map(filter(entities_list('all'),_=='Witch'), query(_,'pos') )  =&gt; [[1082.5, 57, 1243.5]]
     * </pre>
     *
     * <h3><code>first(list,expr(_,_i))</code></h3>
     * <p>Finds and returns the first item in the list that satisfies <code>expr</code>. If sets <code>_</code> for current element value,
     * and <code>_i</code> for index of that element</p>
     * <pre>
     *     first(range(1000,10000), n=_; !first( range(2, sqrt(n)+1), !(n % _) ) )  =&gt; 1009 // first prime after 1000
     * </pre>
     * <p>Notice in the example above, that we needed to rename the outer <code>_</code> to be albe to use in in the inner
     * <code>first</code> call</p>
     *
     *
     * <h3><code>all(list,expr(_,_i))</code></h3>
     * <p>Returns <code>true</code> if all elements on the list satisfy the condition. Its roughly equivalent to
     * <code>all(list,expr) &lt;=&gt; for(list,expr)==length(list)</code>, but works with generators. <code>expr</code>
     * also receives bound <code>_</code> and <code>_i</code> variables</p>
     * <pre>
     *     all([1,2,3], check_prime(_))  =&gt; 1
     *     all(neighbours(x,y,z), _=='stone')  =&gt; 1 // if all neighbours of [x, y, z] are stone
     *     map(filter(rect(0,4,0,1000,0,1000), l(x,y,z)=pos(_); all(rect(x,y,z,1,0,1),_=='bedrock') ), pos(_) )
     *       =&gt; [[-298, 4, -703], [-287, 4, -156], [-269, 4, 104], [242, 4, 250], [-159, 4, 335], [-208, 4, 416], [-510, 4, 546], [376, 4, 806]]
     *         // find all 3x3 bedrock structures in the top bedrock layer
     *     map( filter( rect(0,4,0,1000,1,1000,1000,0,1000), l(x,y,z)=pos(_);
     *             all(rect(x,y,z,1,0,1),_=='bedrock') &amp;&amp; for(rect(x,y-1,z,1,1,1,1,0,1),_=='bedrock')&lt;8),
     *        pos(_) )  =&gt; [[343, 3, -642], [153, 3, -285], [674, 3, 167], [-710, 3, 398]]
     *         // ditto, but requiring at most 7 bedrock block in the 18 blocks below them
     * </pre>
     *
     *
     * <h3><code>for(list,expr(_,_i),exit(_,_i)?)</code></h3>
     * <p>Evaluates expression over list of items from the <code>list</code>, and can optionally stop early if
     * <code>exit</code> expression is specified and evaluates <code>true</code> for a given iteration. Supplies
     * <code>_</code>(value) and <code>_i</code>(iteration number) to both <code>expr</code> and <code>exit</code>.
     * Returns the number of times <code>expr</code> was successful</p>
     * <pre>
     *     check_prime(n) -&gt; !first( range(2, sqrt(n)+1), !(n % _) );
     *        for(range(1000000,1100000),check_prime(_))  =&gt; 7216
     * </pre>
     * <p>From which we can learn that there is 7216 primes between 1M and 1.1M</p>
     * <h3><code>reduce(list,expr(_a,_,_i), initial)</code></h3>
     * <p>Applies <code>expr</code> for each element of the list and saves the result in <code>_a</code> accumulator.
     * Consecutive calls to <code>expr</code> can access that value to apply more values. You also need to specify
     * the initial value to apply for the accumulator</p>
     * <pre>
     *     reduce([1,2,3,4],_+_,0)  =&gt; 10
     *     reduce([1,2,3,4],_*_,1)  =&gt; 24
     * </pre>
     * </div>
     */
    public void ListsLoopsAndHigherOrderFunctions()
    {
        addFunction("l", lv ->
        {
            if (lv.size() == 1 && lv.get(0) instanceof LazyListValue)
                return ListValue.wrap(((LazyListValue) lv.get(0)).unroll());
            return new ListValue.ListConstructorValue(lv);
        });



        addFunction("join", (lv) ->
        {
            if (lv.size() < 2)
                throw new InternalExpressionException("join takes at least 2 arguments");
            String delimiter = lv.get(0).getString();
            List<Value> toJoin;
            if (lv.size()==2 && lv.get(1) instanceof LazyListValue)
            {
                toJoin = ((LazyListValue) lv.get(1)).unroll();

            }
            else if (lv.size() == 2 && lv.get(1) instanceof ListValue)
            {
                toJoin = new ArrayList<>(((ListValue)lv.get(1)).getItems());
            }
            else
            {
                toJoin = lv.subList(1,lv.size());
            }
            return new StringValue(toJoin.stream().map(Value::getString).collect(Collectors.joining(delimiter)));
        });
        addBinaryFunction("split", (d, v) -> {
            String delimiter = d.getString();
            String hwat = v.getString();
            return ListValue.wrap(Arrays.stream(hwat.split(delimiter)).map(StringValue::new).collect(Collectors.toList()));
        });
        addFunction("slice", (lv) -> {

            if (lv.size() != 2 && lv.size() != 3)
                throw new InternalExpressionException("sub takes 2 or 3 arguments");
            Value hwat = lv.get(0);
            long from = getNumericValue(lv.get(1)).getLong();
            long to = -1;
            if (lv.size()== 3)
                to = getNumericValue(lv.get(2)).getLong();
            return hwat.slice(from, to);
        });

        addFunction("sort", (lv) ->
        {
            List<Value> toSort = lv;
            if (lv.size()==1 && lv.get(0) instanceof ListValue)
            {
                toSort = new ArrayList<>(((ListValue)lv.get(0)).getItems());
            }
            Collections.sort(toSort);
            return ListValue.wrap(toSort);
        });

        addLazyFunction("sort_key", 2, (c, t, lv) ->  //get working with iterators
        {
            Value v = lv.get(0).evalValue(c);
            if (!(v instanceof ListValue))
                throw new InternalExpressionException("First argument for sort_key should be a List");
            LazyValue sortKey = lv.get(1);
            //scoping
            LazyValue __ = c.getVariable("_");

            List<Value> toSort = new ArrayList<>(((ListValue) v).getItems());

            Collections.sort(toSort,(v1, v2) -> {
                c.setVariable("_",(cc, tt) -> v1);
                Value ev1 = sortKey.evalValue(c);
                c.setVariable("_",(cc, tt) -> v2);
                Value ev2 = sortKey.evalValue(c);
                return ev1.compareTo(ev2);
            });
            //revering scope
            c.setVariable("_", __);
            return (cc, tt) -> ListValue.wrap(toSort);
        });



        addFunction("range", (lv) ->
        {
            long from = 0;
            long to = 0;
            long step = 1;
            int argsize = lv.size();
            if (argsize == 0 || argsize > 3)
                throw new InternalExpressionException("range accepts from 1 to 3 arguments, not "+argsize);
            to = getNumericValue(lv.get(0)).getLong();
            if (lv.size() > 1)
            {
                from = to;
                to = getNumericValue(lv.get(1)).getLong();
                if (lv.size() > 2)
                {
                    step = getNumericValue(lv.get(2)).getLong();
                }
            }
            return LazyListValue.range(from, to, step);
        });

        addBinaryFunction("element", (v1, v2) -> {
            if (!(v1.getClass().equals(ListValue.class))) // with more list classes, do instanceof ListValue, not instanceof LazyListValue
            {
                throw new InternalExpressionException("First argument of element should be a list");
            }
            List<Value> items = ((ListValue)v1).getItems();
            long index = getNumericValue(v2).getLong();
            int numitems = items.size();
            long range = abs(index)/numitems;
            index += (range+2)*numitems;
            index = index % numitems;
            return items.get((int)index);
        });

        //condition and expression will get a bound 'i'
        //returns last successful expression or false
        // while(cond, limit, expr) => ??
        //replaced with for
        addLazyFunction("while", 3, (c, t, lv) ->
        {
            long limit = getNumericValue(lv.get(1).evalValue(c)).getLong();
            LazyValue condition = lv.get(0);
            LazyValue expr = lv.get(2);
            long i = 0;
            Value lastOne = Value.NULL;
            //scoping
            LazyValue _val = c.getVariable("_");
            c.setVariable("_",(cc, tt) -> new NumericValue(0).bindTo("_"));
            while (i<limit && condition.evalValue(c, Context.BOOLEAN).getBoolean() )
            {
                lastOne = expr.evalValue(c);
                i++;
                long seriously = i;
                c.setVariable("_", (cc, tt) -> new NumericValue(seriously).bindTo("_"));
            }
            //revering scope
            c.setVariable("_", _val);
            Value lastValueNoKidding = lastOne;
            return (cc, tt) -> lastValueNoKidding;
        });

        // loop(Num, expr, exit_condition) => last_value
        // loop(list, expr,
        // expr receives bounded variable '_' indicating iteration
        addLazyFunction("loop", -1, (c, t, lv) ->
        {
            if (lv.size()<2 || lv.size()>3)
            {
                throw new InternalExpressionException("Incorrect number of attributes for loop, should be 2 or 3, not "+lv.size());
            }
            long limit = getNumericValue(lv.get(0).evalValue(c)).getLong();
            Value lastOne = Value.NULL;
            LazyValue expr = lv.get(1);
            LazyValue cond = null;
            if(lv.size() > 2) cond = lv.get(2);
            //scoping
            LazyValue _val = c.getVariable("_");
            for (long i=0; i < limit; i++)
            {
                long whyYouAsk = i;
                c.setVariable("_", (cc, tt) -> new NumericValue(whyYouAsk).bindTo("_"));
                lastOne = expr.evalValue(c);
                if (cond != null && cond.evalValue(c).getBoolean())
                    break;
            }
            //revering scope
            c.setVariable("_", _val);
            Value trulyLastOne = lastOne;
            return (cc, tt) -> trulyLastOne;
        });



        // map(list or Num, expr) => list_results
        // receives bounded variable '_' with the expression
        addLazyFunction("map", -1, (c, t, lv) ->
        {
            if (lv.size()<2 || lv.size()>3)
            {
                throw new InternalExpressionException("Incorrect number of attributes for map, should be 2 or 3, not "+lv.size());
            }

            Value rval= lv.get(0).evalValue(c);

            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of map function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            LazyValue cond = null;
            if(lv.size() > 2) cond = lv.get(2);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int doYouReally = i;
                c.setVariable("_", (cc, tt) -> next.reboundedTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(doYouReally).bindTo("_i"));
                result.add(expr.evalValue(c));
                if (cond != null && cond.evalValue(c).getBoolean())
                    break;
            }
            ((ListValue) rval).fatality();
            LazyValue ret = (cc, tt) -> ListValue.wrap(result);
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return ret;
        });

        // grep(list or num, expr, exit_expr) => list
        // receives bounded variable '_' with the expression, and "_i" with index
        // produces list of values for which the expression is true
        addLazyFunction("filter", -1, (c, t, lv) ->
        {
            if (lv.size()<2 || lv.size()>3)
            {
                throw new InternalExpressionException("Incorrect number of attributes for filter, should be 2 or 3, not "+lv.size());
            }

            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of filter function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            LazyValue cond = null;
            if(lv.size() > 2) cond = lv.get(2);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next.reboundedTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                if(expr.evalValue(c).getBoolean())
                    result.add(next);
                if (cond != null && cond.evalValue(c).getBoolean())
                    break;
            }
            ((ListValue) rval).fatality();
            LazyValue ret = (cc, tt) -> ListValue.wrap(result);
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return ret;
        });

        // first(list, expr) => elem or null
        // receives bounded variable '_' with the expression, and "_i" with index
        // returns first element on the list for which the expr is true
        addLazyFunction("first", 2, (c, t, lv) ->
        {

            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of 'first' function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            Value result = Value.NULL;
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next.reboundedTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                if(expr.evalValue(c).getBoolean())
                {
                    result = next;
                    break;
                }
            }
            //revering scope
            ((ListValue) rval).fatality();
            Value whyWontYouTrustMeJava = result;
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return (cc, tt) -> whyWontYouTrustMeJava;
        });


        // all(list, expr) => boolean
        // receives bounded variable '_' with the expression, and "_i" with index
        // returns true if expr is true for all items
        addLazyFunction("all", 2, (c, t, lv) ->
        {
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of 'all' function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            LazyValue result = LazyValue.TRUE;
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next.reboundedTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                if(!expr.evalValue(c).getBoolean())
                {
                    result = LazyValue.FALSE;
                    break;
                }
            }
            //revering scope
            ((ListValue) rval).fatality();
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return result;
        });


        // similar to map, but returns total number of successes
        // for(list, expr, exit_expr) => success_count
        // can be substituted for first and all, but first is more efficient and all doesn't require knowing list size
        addLazyFunction("for", -1, (c, t, lv) ->
        {
            if (lv.size()<2 || lv.size()>3)
            {
                throw new InternalExpressionException("Incorrect number of attributes for 'for', should be 2 or 3, not "+lv.size());
            }
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("Second argument of 'for' function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            LazyValue cond = null;
            if(lv.size() > 2) cond = lv.get(2);

            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            int successCount = 0;
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next.reboundedTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).bindTo("_i"));
                if(expr.evalValue(c).getBoolean())
                    successCount++;
                if (cond != null && cond.evalValue(c).getBoolean())
                    break;
            }
            //revering scope
            ((ListValue) rval).fatality();
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            long promiseWontChange = successCount;
            return (cc, tt) -> new NumericValue(promiseWontChange);
        });


        // reduce(list, expr, ?acc) => value
        // reduces values in the list with expression that gets accumulator
        // each iteration expr receives acc - accumulator, and '_' - current list value
        // returned value is substituted to the accumulator
        addLazyFunction("reduce", 3, (c, t, lv) ->
        {
            LazyValue expr = lv.get(1);

            Value acc = lv.get(2).evalValue(c);
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of 'reduce' should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();

            if (!iterator.hasNext())
            {
                Value seriouslyWontChange = acc;
                return (cc, tt) -> seriouslyWontChange;
            }

            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _acc = c.getVariable("_a");

            while (iterator.hasNext())
            {
                Value v = iterator.next();
                Value promiseWontChangeYou = acc;
                c.setVariable("_a", (cc, tt) -> promiseWontChangeYou.bindTo("_a"));
                c.setVariable("_", (cc, tt) -> v.reboundedTo("_"));
                acc = expr.evalValue(c);
            }
            //reverting scope
            ((ListValue) rval).fatality();
            c.setVariable("_a", _acc);
            c.setVariable("_", _val);

            Value hopeItsEnoughPromise = acc;
            return (cc, tt) -> hopeItsEnoughPromise;
        });
    }

    /**
     *
     *
     * <h1>System functions</h1>
     * <div style="padding-left: 20px; border-radius: 5px 45px; border:1px solid grey;">
     * <h2>Type conversion functions</h2>
     * <h3><code>bool(expr)</code></h3>
     * <p>Returns a boolean context of the expression. Note that there are no true/false values in
     * scarpet. <code>true</code> is alias of 1, and <code>false</code> is 0</p>
     * <pre>
     * bool(pi) =&gt; 1
     * bool(false) =&gt; 0
     * bool('') =&gt; 0
     * bool(l()) =&gt; 0
     * bool(l('')) =&gt; 1
     * bool('foo') =&gt; 1
     * </pre>
     *
     * <h3><code>number(expr)</code></h3>
     * <p>Returns a numeric context of the expression.
     * Can be used to read numbers from strings</p>
     * <pre>
     * number(null) =&gt; null
     * number(false) =&gt; 0
     * number('') =&gt; null
     * number('3.14') =&gt; 3.14
     * number(l()) =&gt; 0
     * number(l('')) =&gt; 1
     * number('foo') =&gt; null
     * number('3bar') =&gt; null
     * number('2')+number('2') =&gt; 4
     * </pre>
     *
     * <h3><code>str(expr, params? ... )</code></h3>
     *
     * <p>Returns a formatted string representing expression.
     * Accepts formatting style accepted by <code>String.format</code>.
     * Supported types (with <code>"%?"</code> syntax):</p>
     * <ul>
     *     <li>d, o, x: integers</li>
     *     <li>a, e, f, g: floats</li>
     *     <li>b: booleans</li>
     *     <li>s: strings</li>
     * </ul>
     *
     * <pre>
     * str(null) =&gt; null
     * str(false) =&gt; 0
     * str('') =&gt; null
     * str('3.14') =&gt; 3.14
     * str(l()) =&gt; 0
     * str(l('')) =&gt; 1
     * str('foo') =&gt; null
     * str('3bar') =&gt; null
     * str(2)+str(2) =&gt; 22
     * str('pi: %.2f',pi) =&gt; 3.14
     * </pre>
     *
     * <hr>
     * <h2>Auxiliary functions</h2>
     *
     * <h3><code>length(expr)</code></h3>
     * <p>Returns length of the expression, the length of the string,
     * the length of the integer part of the number, or length of the list</p>
     * <pre>
     * length(pi) =&gt; 1
     * length(pi*pi) =&gt; 1
     * length(pi^pi) =&gt; 2
     * length(l()) =&gt; 0
     * length(l(1,2,3)) =&gt; 3
     * length('') =&gt; 0
     * length('foo') =&gt; 3
     * </pre>
     *
     * <h3><code>rand(expr)</code></h3>
     * <p>returns a random number from <code>0.0</code>
     * (inclusive) to <code>expr</code> (exclusive).
     * In boolean context (in conditions, boolean functions, or <code>bool</code>), returns
     * false if the randomly selected value is less than 0</p>
     * <pre>
     * map(range(10), floor(rand(10))) =&gt; [5, 8, 0, 6, 9, 3, 9, 9, 1, 8]
     * map(range(10), bool(rand(2))) =&gt; [1, 1, 1, 0, 0, 1, 1, 0, 0, 0]
     * map(range(10), str('%.1f',rand(_))) =&gt; [0.0, 0.4, 0.6, 1.9, 2.8, 3.8, 5.3, 2.2, 1.6, 5.6]
     * </pre>
     *
     * <h3><code>print(expr)</code></h3>
     * <p>prints the value of the expression to chat.
     * Passes the result of the argument to the output unchanged, so <code>print</code>statements can
     * be weaved in code to debug programming issues</p>
     * <pre>
     *     print('foo') =&gt; results in foo, prints: foo
     *     a = 1; print(a = 5) =&gt; results in 5, prints: 5
     *     a = 1; print(a) = 5 =&gt; results in 5, prints: 1
     *     print('pi = '+pi) =&gt; prints: pi = 3.141592653589793
     *     print(str('pi = %.2f',pi)) =&gt; prints: pi = 3.14
     * </pre>
     *
     * <h3><code>sleep(expr)</code></h3>
     * <p>Halts the execution of the program (and the game itself) for <code>expr</code> milliseconds.
     * All in all, its better to use <code>tick(expr)</code> to let the game do its job while the program waits</p>
     * <pre>sleep(50)</pre>
     * <h3><code>time()</code></h3>
     * <p>Returns the number of milliseconds since 'some point',
     * like Java's <code>System.nanoTime()</code>. It returns a float, which has 1 microsecond precision
     * (0.001 ms)</p>
     * <pre>
     *     start_time = time();
     *     flip_my_world_upside_down();
     *     print(str('this took %d milliseconds',time()-start_time))
     * </pre>
     *
     * <hr>
     * <h2>Access to variables and stored functions</h2>
     *
     * <h3><code>var(expr)</code></h3>
     * <p>Returns the variable under the name of the string value of the expression. Allows to
     * manipulate variables in more programmatic manner, which allows to use local variable set with a
     * hash map type key-value access, can also be used with global variables</p>
     * <pre>
     *     a = 1; var('a') = 'foo'; a =&gt; a == 'foo'
     * </pre>
     *
     * <h3><code>undef(expr)</code></h3>
     * <p>Removes all bindings of a variable with a name of <code>expr</code>.
     * Removes also all function definitions with that name. It can affect global variable pool, and local variable set
     * for a particular function which will become invalid ........, </p>
     * <pre>
     *     inc(i) -&gt; i+1; foo = 5; inc(foo) =&gt; 6
     *     inc(i) -&gt; i+1; foo = 5; undef('foo'); inc(foo) =&gt; 1
     *     inc(i) -&gt; i+1; foo = 5; undef('inc'); undef('foo'); inc(foo) =&gt; Error: Function inc is not defined yet at pos 53
     *     undef('pi')  =&gt; bad idea - removes hidden variable holding the pi value
     *     undef('true')  =&gt; even worse idea, unbinds global true value, all references to true would now refer to the default 0
     * </pre>
     * <h3><code>vars(prefix)</code></h3>
     * <p>It returns all names of variables from local scope (if prefix does not start with 'global')
     * or global variables (otherwise).
     * Here is a larger example that uses combination of <code>vars</code> and <code>var</code> functions to
     * be used for object counting</p>
     * <pre>
     * /script run
     * $ count_blocks(ent) -&gt; (
     * $   l(cx, cy, cz) = query(ent, 'pos');
     * $   scan(cx, cy, cz, 16, 16, 16, var('count_'+_) += 1);
     * $   for ( sort_key( vars('count_'), -var(_)),
     * $     print(str( '%s: %d', slice(_,6), var(_) ))
     * $   )
     * $ )
     *
     * /script run count_blocks(player())
     * </pre>
     *
     * </div>
     */
    public void SystemFunctions()
    {
        addLazyFunction("bool", 1, (c, t, lv) -> {
            return (cc, tt) -> new NumericValue(lv.get(0).evalValue(c, Context.BOOLEAN).getBoolean());
        });
        addUnaryFunction("number", v -> {
            if (v instanceof NumericValue)
                return v;
            double res = v.readNumber();
            if (Double.isNaN(res))
                return Value.NULL;
            return new NumericValue(v.readNumber());
        });
        addFunction("str", lv ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("str requires at least one argument");
            String format = lv.get(0).getString();
            if (lv.size() == 1)
                return new StringValue(format);
            List<Object> args = new ArrayList<>();
            Matcher m = formatPattern.matcher(format);
            int argIndex = 1;
            for (int i = 0, len = format.length(); i < len; ) {
                if (m.find(i)) {
                    // Anything between the start of the string and the beginning
                    // of the format specifier is either fixed text or contains
                    // an invalid format string.
                    // [[scarpet]] but we skip it and let the String.format fail
                    char fmt = m.group(6).toLowerCase().charAt(0);
                    if (fmt == 's')
                    {
                        if (argIndex >= lv.size())
                            throw new InternalExpressionException("Not enough arguments for "+m.group(0));
                        args.add(lv.get(argIndex).getString());
                        argIndex++;
                    }
                    else if (fmt == 'd' || fmt == 'o' || fmt == 'x')
                    {
                        if (argIndex >= lv.size())
                            throw new InternalExpressionException("Not enough arguments for "+m.group(0));
                        args.add(lv.get(argIndex).readInteger());
                        argIndex++;
                    }
                    else if (fmt == 'a' || fmt == 'e' || fmt == 'f' || fmt == 'g')
                    {
                        if (argIndex >= lv.size())
                            throw new InternalExpressionException("Not enough arguments for "+m.group(0));
                        args.add(lv.get(argIndex).readNumber());
                        argIndex++;
                    }
                    else if (fmt == 'b')
                    {
                        if (argIndex >= lv.size())
                            throw new InternalExpressionException("Not enough arguments for "+m.group(0));
                        args.add(lv.get(argIndex).getBoolean());
                        argIndex++;
                    }
                    else if (fmt == '%')
                    {
                        //skip /%%
                        ;
                    }
                    else
                    {
                        throw new InternalExpressionException("format not supported: "+m.group(6));
                    }

                    i = m.end();
                } else {
                    // No more valid format specifiers.  Check for possible invalid
                    // format specifiers.
                    // [[scarpet]] but we skip it and let the String.format fail
                    break;
                }
            }
            try
            {
                return new StringValue(String.format(format, args.toArray()));
            }
            catch (IllegalFormatException ife)
            {
                throw new InternalExpressionException("Illegal string format: "+ife.getMessage());
            }
        });


        addUnaryFunction("length", v -> new NumericValue(v.length()));
        addLazyFunction("rand", 1, (c, t, lv) -> {
            Value argument = lv.get(0).evalValue(c);
            if (argument instanceof ListValue)
            {
                List<Value> list = ((ListValue) argument).getItems();
                return (cc, tt) -> list.get(randomizer.nextInt(list.size()));
            }
            if (t == Context.BOOLEAN)
            {
                double rv = getNumericValue(argument).getDouble()*randomizer.nextFloat();
                return (cc, tt) -> rv<1.0D?Value.FALSE:Value.TRUE;
            }

            return (cc, tt) -> new NumericValue(getNumericValue(argument).getDouble()*randomizer.nextFloat());
        });

        addUnaryFunction("print", (v) ->
        {
            System.out.println(v.getString());
            return v; // pass through for variables
        });
        addUnaryFunction("sleep", (v) ->
        {
            long time = getNumericValue(v).getLong();
            try
            {
                Thread.sleep(time);
                Thread.yield();
            }
            catch (InterruptedException ignored) { }
            return v; // pass through for variables
        });
        addLazyFunction("time", 0, (c, t, lv) ->
                (cc, tt) -> new NumericValue((System.nanoTime()/1000)/1000.0));

        addLazyFunction("var", 1, (c, t, lv) -> {
            String varname = lv.get(0).evalValue(c).getString();
            if (!c.isAVariable(varname))
                c.setVariable(varname, (_c, _t ) -> Value.ZERO.reboundedTo(varname));
            return c.getVariable(varname);
        });

        addLazyFunction("undef", 1, (c, t, lv) ->
        {
            String varname = lv.get(0).evalValue(c).getString();
            if (varname.startsWith("_"))
                throw new InternalExpressionException("Cannot replace local built-in variables, i.e. those that start with '_'");
            if (varname.endsWith("*"))
            {
                varname = varname.replaceAll("\\*+$", "");
                for (String key: globalFunctions.keySet())
                {
                    if (key.startsWith(varname)) globalFunctions.remove(key);
                }
                for (String key: globalVariables.keySet())
                {
                    if (key.startsWith(varname)) globalVariables.remove(key);
                }
                c.clearAll(varname);
            }
            else
            {
                globalFunctions.remove(varname);
                globalVariables.remove(varname);
                c.delVariable(varname);
            }
            return (cc, tt) -> Value.NULL;
        });


        addLazyFunction("vars", 1, (c, t, lv) -> {
            String prefix = lv.get(0).evalValue(c).getString();
            List<Value> values = new ArrayList<>();
            if (prefix.startsWith("global"))
            {
                for (String k: globalVariables.keySet())
                {
                    if (k.startsWith(prefix))
                        values.add(new StringValue(k));
                }
            }
            else
            {
                for (String k: c.getAllVariableNames())
                {
                    if (k.startsWith(prefix))
                        values.add(new StringValue(k));
                }
            }
            return (cc, tt) -> ListValue.wrap(values);
        });

    }

    /**
     * @param expression .
     */
    public Expression(String expression)
    {
        expression = expression.trim().replaceAll(";+$", "");
        this.expression = expression.replaceAll("\\$", "\n");
        Constants();
        UserDefinedFunctionsAndControlFlow();
        Operators();
        ArithmeticOperations();
        SystemFunctions();
        ListsLoopsAndHigherOrderFunctions();
    }


    private List<Tokenizer.Token> shuntingYard(String expression)
    {
        List<Tokenizer.Token> outputQueue = new ArrayList<>();
        Stack<Tokenizer.Token> stack = new Stack<>();

        Tokenizer tokenizer = new Tokenizer(this, expression);

        Tokenizer.Token lastFunction = null;
        Tokenizer.Token previousToken = null;
        while (tokenizer.hasNext())
        {
            Tokenizer.Token token;
            try
            {
                token = tokenizer.next();
            }
            catch (StringIndexOutOfBoundsException e)
            {
                throw new ExpressionException("Script ended prematurely");
            }
            switch (token.type)
            {
                case STRINGPARAM:
                    //stack.push(token); // changed that so strings are treated like literals
                    //break;
                case LITERAL:
                case HEX_LITERAL:
                    if (previousToken != null && (
                            previousToken.type == Tokenizer.Token.TokenType.LITERAL ||
                                    previousToken.type == Tokenizer.Token.TokenType.HEX_LITERAL ||
                                    previousToken.type == Tokenizer.Token.TokenType.STRINGPARAM))
                    {
                        throw new ExpressionException(this, token, "Missing operator");
                    }
                    outputQueue.add(token);
                    break;
                case VARIABLE:
                    outputQueue.add(token);
                    break;
                case FUNCTION:
                    stack.push(token);
                    lastFunction = token;
                    break;
                case COMMA:
                    if (previousToken != null && previousToken.type == Tokenizer.Token.TokenType.OPERATOR)
                    {
                        throw new ExpressionException(this, previousToken, "Missing parameter(s) for operator ");
                    }
                    while (!stack.isEmpty() && stack.peek().type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        if (lastFunction == null)
                        {
                            throw new ExpressionException(this, token, "Unexpected comma");
                        }
                        else
                        {
                            throw new ExpressionException(this, lastFunction, "Parse error for function");
                        }
                    }
                    break;
                case OPERATOR:
                {
                    if (previousToken != null
                            && (previousToken.type == Tokenizer.Token.TokenType.COMMA || previousToken.type == Tokenizer.Token.TokenType.OPEN_PAREN))
                    {
                        throw new ExpressionException(this, token, "Missing parameter(s) for operator '" + token+"'");
                    }
                    ILazyOperator o1 = operators.get(token.surface);
                    if (o1 == null)
                    {
                        throw new ExpressionException(this, token, "Unknown operator '" + token + "'");
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case UNARY_OPERATOR:
                {
                    if (previousToken != null && previousToken.type != Tokenizer.Token.TokenType.OPERATOR
                            && previousToken.type != Tokenizer.Token.TokenType.COMMA && previousToken.type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        throw new ExpressionException(this, token, "Invalid position for unary operator " + token );
                    }
                    ILazyOperator o1 = operators.get(token.surface);
                    if (o1 == null)
                    {
                        throw new ExpressionException(this, token, "Unknown unary operator '" + token.surface.substring(0, token.surface.length() - 1) + "'");
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case OPEN_PAREN:
                    if (previousToken != null)
                    {
                        if (previousToken.type == Tokenizer.Token.TokenType.LITERAL || previousToken.type == Tokenizer.Token.TokenType.CLOSE_PAREN
                                || previousToken.type == Tokenizer.Token.TokenType.VARIABLE
                                || previousToken.type == Tokenizer.Token.TokenType.HEX_LITERAL)
                        {
                            // Implicit multiplication, e.g. 23(a+b) or (a+b)(a-b)
                            Tokenizer.Token multiplication = new Tokenizer.Token();
                            multiplication.append("*");
                            multiplication.type = Tokenizer.Token.TokenType.OPERATOR;
                            stack.push(multiplication);
                        }
                        // if the ( is preceded by a valid function, then it
                        // denotes the start of a parameter list
                        if (previousToken.type == Tokenizer.Token.TokenType.FUNCTION)
                        {
                            outputQueue.add(token);
                        }
                    }
                    stack.push(token);
                    break;
                case CLOSE_PAREN:
                    if (previousToken != null && previousToken.type == Tokenizer.Token.TokenType.OPERATOR)
                    {
                        throw new ExpressionException(this, previousToken, "Missing parameter(s) for operator " + previousToken);
                    }
                    while (!stack.isEmpty() && stack.peek().type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        throw new ExpressionException("Mismatched parentheses");
                    }
                    stack.pop();
                    if (!stack.isEmpty() && stack.peek().type == Tokenizer.Token.TokenType.FUNCTION)
                    {
                        outputQueue.add(stack.pop());
                    }
            }
            previousToken = token;
        }

        while (!stack.isEmpty())
        {
            Tokenizer.Token element = stack.pop();
            if (element.type == Tokenizer.Token.TokenType.OPEN_PAREN || element.type == Tokenizer.Token.TokenType.CLOSE_PAREN)
            {
                throw new ExpressionException(this, element, "Mismatched parentheses");
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    private void shuntOperators(List<Tokenizer.Token> outputQueue, Stack<Tokenizer.Token> stack, ILazyOperator o1)
    {
        Tokenizer.Token nextToken = stack.isEmpty() ? null : stack.peek();
        while (nextToken != null
                && (nextToken.type == Tokenizer.Token.TokenType.OPERATOR
                || nextToken.type == Tokenizer.Token.TokenType.UNARY_OPERATOR)
                && ((o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(nextToken.surface).getPrecedence())
                || (o1.getPrecedence() < operators.get(nextToken.surface).getPrecedence())))
        {
            outputQueue.add(stack.pop());
            nextToken = stack.isEmpty() ? null : stack.peek();
        }
    }

    Value eval(Context c)
    {
        return eval(c, Context.NONE);
    }
    private Value eval(Context c, Integer expectedType)
    {
        if (ast == null)
        {
            ast = getAST();
        }
        try
        {
            return ast.evalValue(c, expectedType);
        }
        catch (ExitStatement exit)
        {
            return exit.retval;
        }
        catch (StackOverflowError ignored)
        {
            throw new ExpressionException("Your thoughts are too deep");
        }
        catch (InternalExpressionException exc)
        {
            throw new ExpressionException("Your expression result is incorrect:"+exc.getMessage());
        }
        catch (ArithmeticException exc)
        {
            throw new ExpressionException("The final result is incorrect, "+exc.getMessage());
        }
    }

    private LazyValue getAST()
    {
        Stack<LazyValue> stack = new Stack<>();
        List<Tokenizer.Token> rpn = shuntingYard(this.expression);
        validate(rpn);
        for (final Tokenizer.Token token : rpn)
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                {
                    final LazyValue value = stack.pop();
                    LazyValue result = (c, t) -> operators.get(token.surface).lazyEval(c, t, this, token, value, null).evalValue(c);
                    stack.push(result);
                    break;
                }
                case OPERATOR:
                    final LazyValue v1 = stack.pop();
                    final LazyValue v2 = stack.pop();
                    LazyValue result = (c,t) -> operators.get(token.surface).lazyEval(c, t,this, token, v2, v1).evalValue(c);
                    stack.push(result);
                    break;
                case VARIABLE:
                    stack.push((c, t) ->
                    {
                        if (!c.isAVariable(token.surface)) // new variable
                        {
                            c.setVariable(token.surface, (cc, tt ) -> Value.ZERO.reboundedTo(token.surface));
                        }
                        LazyValue lazyVariable = c.getVariable(token.surface);
                        return lazyVariable.evalValue(c);
                    });
                    break;
                case FUNCTION:
                    String name = token.surface.toLowerCase(Locale.ROOT);
                    ILazyFunction f;
                    ArrayList<LazyValue> p;
                    boolean isKnown = functions.containsKey(name); // globals will be evaluated lazily, not at compile time via .
                    if (isKnown)
                    {
                        f = functions.get(name);
                        p = new ArrayList<>(!f.numParamsVaries() ? f.getNumParams() : 0);
                    }
                    else // potentially unknown function or just unknown function
                    {
                        f = functions.get(".");
                        p = new ArrayList<>();
                    }
                    // pop parameters off the stack until we hit the start of
                    // this function's parameter list
                    while (!stack.isEmpty() && stack.peek() != LazyValue.PARAMS_START)
                    {
                        p.add(0, stack.pop());
                    }
                    if (!isKnown) p.add( (c, t) -> new StringValue(name));

                    if (stack.peek() == LazyValue.PARAMS_START)
                    {
                        stack.pop();
                    }

                    stack.push((c, t) -> f.lazyEval(c, t, this, token, p).evalValue(c));
                    break;
                case OPEN_PAREN:
                    stack.push(LazyValue.PARAMS_START);
                    break;
                case LITERAL:
                    stack.push((c, t) ->
                    {
                        if (token.surface.equalsIgnoreCase("NULL")) // TODO possibly not neded if works with euler and pi
                            return Value.NULL;
                        try
                        {
                            return new NumericValue(token.surface);
                        }
                        catch (NumberFormatException exception)
                        {
                            throw new ExpressionException(this, token, "Not a number");
                        }

                    });
                    break;
                case STRINGPARAM:
                    stack.push((c, t) -> new StringValue(token.surface) ); // was originally null
                    break;
                case HEX_LITERAL:
                    stack.push((c, t) -> new NumericValue(new BigInteger(token.surface.substring(2), 16).doubleValue()));
                    break;
                default:
                    throw new ExpressionException(this, token, "Unexpected token '" + token.surface + "'");
            }
        }
        return stack.pop();
    }

    private void validate(List<Tokenizer.Token> rpn)
    {
        /*-
         * Thanks to Norman Ramsey:
         * http://http://stackoverflow.com/questions/789847/postfix-notation-validation
         */
        // each push on to this stack is a new function scope, with the value of
        // each
        // layer on the stack being the count of the number of parameters in
        // that scope
        Stack<Integer> stack = new Stack<>();

        // push the 'global' scope
        stack.push(0);

        for (final Tokenizer.Token token : rpn)
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                    if (stack.peek() < 1)
                    {
                        throw new ExpressionException(this, token, "Missing parameter(s) for operator " + token);
                    }
                    break;
                case OPERATOR:
                    if (stack.peek() < 2)
                    {
                        if (token.surface.equalsIgnoreCase(";"))
                        {
                            throw new ExpressionException(this, token, "Unnecessary semicolon");
                        }
                        throw new ExpressionException(this, token, "Missing parameter(s) for operator " + token);
                    }
                    // pop the operator's 2 parameters and add the result
                    stack.set(stack.size() - 1, stack.peek() - 2 + 1);
                    break;
                case FUNCTION:
                    ILazyFunction f = functions.get(token.surface.toLowerCase(Locale.ROOT));// don't validate global - userdef functions
                    int numParams = stack.pop();
                    if (f != null && !f.numParamsVaries() && numParams != f.getNumParams())
                    {
                        throw new ExpressionException(this, token, "Function " + token + " expected " + f.getNumParams() + " parameters, got " + numParams);
                    }
                    if (stack.size() <= 0)
                    {
                        throw new ExpressionException(this, token, "Too many function calls, maximum scope exceeded");
                    }
                    // push the result of the function
                    stack.set(stack.size() - 1, stack.peek() + 1);
                    break;
                case OPEN_PAREN:
                    stack.push(0);
                    break;
                default:
                    stack.set(stack.size() - 1, stack.peek() + 1);
            }
        }

        if (stack.size() > 1)
        {
            throw new ExpressionException("Too many unhandled function parameter lists");
        }
        else if (stack.peek() > 1)
        {
            throw new ExpressionException("Too many numbers or variables");
        }
        else if (stack.peek() < 1)
        {
            throw new ExpressionException("Empty expression");
        }
    }

    @FunctionalInterface
    interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }
    @FunctionalInterface
    interface QuadFunction<A, B, C, D, R> { R apply(A a, B b, C c, D d);}
    @FunctionalInterface
    interface QuinnFunction<A, B, C, D, E, R> { R apply(A a, B b, C c, D d, E e);}
    @FunctionalInterface
    interface SexFunction<A, B, C, D, E, F, R> { R apply(A a, B b, C c, D d, E e, F f);}

    private interface ILazyFunction
    {
        int getNumParams();

        boolean numParamsVaries();

        LazyValue lazyEval(Context c, Integer type, Expression expr, Tokenizer.Token token, List<LazyValue> lazyParams);
        // lazy function has a chance to change execution based on contxt
    }

    private interface IFunction extends ILazyFunction
    {
        Value eval(List<Value> parameters);
    }

    private interface ILazyOperator
    {
        int getPrecedence();

        boolean isLeftAssoc();

        LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2);
    }

    private interface IOperator extends ILazyOperator
    {
        Value eval(Value v1, Value v2);
    }

    abstract static class UserDefinedFunction extends AbstractLazyFunction implements ILazyFunction
    {
        protected List<String> arguments;
        protected Expression expression;
        protected Tokenizer.Token token;
        UserDefinedFunction(List<String> args, Expression expr, Tokenizer.Token t)
        {
            super(args.size());
            arguments = args;
            expression = expr;
            token = t;
        }
        public List<String> getArguments()
        {
            return arguments;
        }
        public Expression getExpression()
        {
            return expression;
        }
        public Tokenizer.Token getToken()
        {
            return token;
        }
    }

    private abstract static class AbstractLazyFunction implements ILazyFunction
    {
        protected String name;
        int numParams;

        AbstractLazyFunction(int numParams)
        {
            this.numParams = numParams;
        }


        public String getName() {
            return name;
        }

        public int getNumParams() {
            return numParams;
        }

        public boolean numParamsVaries() {
            return numParams < 0;
        }
    }

    private abstract static class AbstractFunction extends AbstractLazyFunction implements IFunction
    {
        AbstractFunction(int numParams) {
            super(numParams);
        }

        @Override
        public LazyValue lazyEval(Context cc, Integer type, Expression e, Tokenizer.Token t, final List<LazyValue> lazyParams)
        {
            try
            {
                return new LazyValue()
                { // eager evaluation always ignores the required type and evals params by none default
                    private List<Value> params;

                    public Value evalValue(Context c, Integer type) {
                        return AbstractFunction.this.eval(getParams(c));
                    }

                    private List<Value> getParams(Context c) {
                        if (params == null) {
                            params = new ArrayList<>();
                            for (LazyValue lazyParam : lazyParams) {
                                params.add(lazyParam.evalValue(c)); // none type default by design
                            }
                        }
                        return params;
                    }
                };
            }
            catch (InternalExpressionException exc)
            {
                throw new ExpressionException(e, t, exc.getMessage());
            }
        }
    }

    private abstract static class AbstractLazyOperator implements ILazyOperator
    {
        int precedence;

        boolean leftAssoc;

        AbstractLazyOperator(int precedence, boolean leftAssoc) {
            this.precedence = precedence;
            this.leftAssoc = leftAssoc;
        }

        public int getPrecedence() {
            return precedence;
        }

        public boolean isLeftAssoc() {
            return leftAssoc;
        }

    }

    private abstract static class AbstractOperator extends AbstractLazyOperator implements IOperator
    {

        AbstractOperator(int precedence, boolean leftAssoc) {
            super(precedence, leftAssoc);
        }

        @Override
        public LazyValue lazyEval(Context c_ignored, Integer type, Expression e, Tokenizer.Token t, final LazyValue v1, final LazyValue v2)
        {
            try
            {
                return (c, type_ignored) -> AbstractOperator.this.eval(v1.evalValue(c), v2.evalValue(c));
            }
            catch (InternalExpressionException exc)
            {
                throw new ExpressionException(e, t, exc.getMessage());
            }
            catch (ArithmeticException exc)
            {
                throw new ExpressionException(e, t, "Your math is wrong, "+exc.getMessage());
            }
        }
    }

    private abstract static class AbstractUnaryOperator extends AbstractOperator
    {
        AbstractUnaryOperator(int precedence, boolean leftAssoc) {
            super(precedence, leftAssoc);
        }

        @Override
        public LazyValue lazyEval(Context cc, Integer type, Expression e, Tokenizer.Token t, final LazyValue v1, final LazyValue v2)
        {
            try
            {
                if (v2 != null)
                {
                    throw new ExpressionException(e, t, "Did not expect a second parameter for unary operator");
                }
                return (c, ignored_type) -> AbstractUnaryOperator.this.evalUnary(v1.evalValue(c));
            }
            catch (InternalExpressionException exc) // might not actually throw it
            {
                throw new ExpressionException(e, t, exc.getMessage());
            }
            catch (ArithmeticException exc)
            {
                throw new ExpressionException(e, t, "Your math is wrong, "+exc.getMessage());
            }
        }

        @Override
        public Value eval(Value v1, Value v2)
        {
            throw new ExpressionException("Shouldn't end up here");
        }

        public abstract Value evalUnary(Value v1);
    }
}
