package edu.kit.joana.wala.eval;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.classLoader.DexIRFactory;
import com.ibm.wala.dalvik.ipa.callgraph.impl.DexEntryPoint;
import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.SDGBuilderConfig;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.util.pointsto.WalaPointsToUtil;

public class RunStaticCallBug {

	private SDG sdg;
	private CallGraph callGraph;
	private PointerAnalysis<InstanceKey> ptrAnalysis;
	
	private static final String[] ENTRIES =
//	new String[] { 
//		"com.google.ads.ad.a([B[B)V"
//	};
new String[]{
   "gnu.math.Dimensions.toString()Ljava/lang/String;",
   "gnu.lists.TreeList.<init>(Lgnu/lists/TreeList;II)V",
   "gnu.lists.PairWithPosition.<init>(Lgnu/text/SourceLocator;Ljava/lang/Object;Ljava/lang/Object;)V",
   "gnu.lists.AbstractSequence.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.VideoPlayer.onError(Landroid/media/MediaPlayer;II)Z",
   "gnu.kawa.functions.ObjectFormat.<init>(ZI)V",
   "gnu.mapping.OutPort.print(I)V",
   "gnu.kawa.reflect.SlotGet.<clinit>()V",
   "gnu.text.PadFormat.<init>(Ljava/text/Format;ICI)V",
   "gnu.expr.ErrorExp.<init>(Ljava/lang/String;Lgnu/expr/Compilation;)V",
   "gnu.mapping.ProcedureN.<clinit>()V",
   "gnu.bytecode.Type.<init>(Lgnu/bytecode/Type;)V",
   "gnu.kawa.functions.Format.<clinit>()V",
   "gnu.text.SourceMessages.<clinit>()V",
   "gnu.mapping.CharArrayOutPort.toString()Ljava/lang/String;",
   "gnu.math.ExponentialFormat.parseObject(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Object;",
   "com.google.appinventor.components.runtime.util.YailList.toString()Ljava/lang/String;",
   "gnu.kawa.util.GeneralHashTable.<init>(I)V",
   "gnu.text.Lexer.reset()V",
   "appinventor.ai_ariseo.OFWapp.Screen11$frame.<init>()V",
   "com.google.appinventor.components.runtime.util.YailNumberToString.<clinit>()V",
   "kawa.lang.Syntax.<init>(Ljava/lang/Object;)V",
   "gnu.expr.ModuleInfo.toString()Ljava/lang/String;",
   "appinventor.ai_ariseo.OFWapp.Screen1.run()V",
   "com.google.appinventor.components.runtime.collect.Maps.<init>()V",
   "gnu.math.Duration.<init>()V",
   "gnu.kawa.functions.LispIndentFormat.<init>()V",
   "gnu.math.Complex.doubleValue()D",
   "gnu.mapping.TtyInPort.<init>(Ljava/io/Reader;Lgnu/text/Path;Lgnu/mapping/OutPort;)V",
   "gnu.kawa.functions.LispIterationFormat.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.Form.onPause()V",
   "gnu.text.WriterManager.run()V",
   "kawa.lang.Continuation.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.Form.onCreateDialog(I)Landroid/app/Dialog;",
   "gnu.kawa.functions.LispEscapeFormat.<clinit>()V",
   "gnu.math.NamedUnit.<init>(Ljava/lang/String;DLgnu/math/Unit;)V",
   "gnu.kawa.util.HashNode.<init>(Ljava/lang/Object;Ljava/lang/Object;)V",
   "gnu.math.NamedUnit.<init>(Ljava/lang/String;Lgnu/math/DQuantity;)V",
   "gnu.mapping.CharArrayOutPort.close()V",
   "kawa.lib.kawa.hashtable$HashTable.clone()Ljava/lang/Object;",
   "com.google.appinventor.components.runtime.LinearLayout.<init>(Landroid/content/Context;I)V",
   "gnu.math.DFloNum.toString()Ljava/lang/String;",
   "gnu.math.Numeric.<init>()V",
   "gnu.math.IntNum.<init>(I)V",
   "appinventor.ai_ariseo.OFWapp.Screen4.<clinit>()V",
   "gnu.kawa.functions.LispNewlineFormat.<clinit>()V",
   "gnu.lists.FString.<init>(I)V",
   "gnu.mapping.NamedLocation.hashCode()I",
   "gnu.kawa.xml.NodeType.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.WebViewActivity$1.<init>(Lcom/google/appinventor/components/runtime/WebViewActivity;)V",
   "gnu.mapping.OutPort.<init>(Ljava/io/Writer;Z)V",
   "gnu.expr.Keyword.toString()Ljava/lang/String;",
   "gnu.math.Unit.<clinit>()V",
   "gnu.lists.PrintConsumer.<init>(Ljava/io/Writer;Z)V",
   "kawa.lang.TemplateScope.toString()Ljava/lang/String;",
   "gnu.expr.LambdaExp.<init>(Lgnu/expr/Expression;)V",
   "gnu.text.PrettyWriter.<init>(Ljava/io/Writer;)V",
   "gnu.mapping.TtyInPort.read()I",
   "gnu.mapping.PropertyKey.<init>(Ljava/lang/String;)V",
   "gnu.text.PadFormat.<init>(Ljava/text/Format;I)V",
   "kawa.lang.SyntaxRule.<init>()V",
   "gnu.kawa.functions.LispChoiceFormat.<init>()V",
   "gnu.mapping.CharArrayInPort.<clinit>()V",
   "gnu.xml.XMLPrinter.write(Ljava/lang/String;II)V",
   "gnu.math.IntFraction.<init>(Lgnu/math/IntNum;Lgnu/math/IntNum;)V",
   "gnu.math.IntNum.intValue()I",
   "gnu.xml.XMLPrinter.<clinit>()V",
   "gnu.math.ExponentialFormat.<clinit>()V",
   "gnu.lists.FString.toString()Ljava/lang/String;",
   "gnu.lists.FString.<init>(Ljava/lang/CharSequence;II)V",
   "gnu.text.LineBufferedReader.<init>(Ljava/io/InputStream;)V",
   "gnu.math.DComplex.doubleValue()D",
   "appinventor.ai_ariseo.OFWapp.Screen4.<init>()V",
   "gnu.mapping.UnboundLocationException.toString()Ljava/lang/String;",
   "gnu.text.LineBufferedReader.read([CII)I",
   "appinventor.ai_ariseo.OFWapp.Screen10$frame.<init>()V",
   "gnu.math.IntNum.hashCode()I",
   "gnu.mapping.InPort.<init>(Ljava/io/InputStream;Lgnu/text/Path;)V",
   "gnu.text.LiteralFormat.<init>([C)V",
   "gnu.text.RomanIntegerFormat.parse(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Number;",
   "kawa.lang.SyntaxRules.<init>()V",
   "gnu.expr.Keyword.<clinit>()V",
   "gnu.lists.ConsumerWriter.flush()V",
   "gnu.math.Complex.longValue()J",
   "gnu.text.URIStringPath.<init>(Ljava/net/URI;Ljava/lang/String;)V",
   "gnu.expr.LambdaExp.<init>(I)V",
   "gnu.mapping.Namespace.<init>()V",
   "com.google.appinventor.components.runtime.LinearLayout$1.onMeasure(II)V",
   "gnu.lists.LList.<clinit>()V",
   "gnu.lists.FVector.<clinit>()V",
   "com.google.appinventor.components.runtime.util.ViewUtil.<init>()V",
   "gnu.expr.PrimProcedure.<init>(Lgnu/bytecode/Method;CLgnu/expr/Language;)V",
   "gnu.text.Char.equals(Ljava/lang/Object;)Z",
   "gnu.expr.KawaConvert.<init>()V",
   "gnu.kawa.functions.IsEqual.<init>(Lgnu/expr/Language;Ljava/lang/String;)V",
   "kawa.lang.AutoloadSyntax.<init>(Ljava/lang/String;Ljava/lang/String;Lgnu/mapping/Environment;)V",
   "gnu.math.FixedRealFormat.format(Ljava/lang/Object;Ljava/lang/StringBuffer;Ljava/text/FieldPosition;)Ljava/lang/StringBuffer;",
   "gnu.bytecode.Variable.toString()Ljava/lang/String;",
   "gnu.mapping.TtyInPort.<init>(Ljava/io/InputStream;Lgnu/text/Path;Lgnu/mapping/OutPort;)V",
   "gnu.text.LiteralFormat.parseObject(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Object;",
   "com.google.appinventor.components.runtime.ListPickerActivity.onCreate(Landroid/os/Bundle;)V",
   "gnu.mapping.ProcedureN.<init>()V",
   "gnu.kawa.functions.LispCharacterFormat.<init>()V",
   "gnu.mapping.TtyInPort.read([CII)I",
   "appinventor.ai_ariseo.OFWapp.Screen4.run()V",
   "gnu.math.DateTime.toString()Ljava/lang/String;",
   "gnu.lists.FString.equals(Ljava/lang/Object;)Z",
   "com.google.appinventor.components.runtime.EventDispatcher$EventClosure.<init>(Ljava/lang/String;Ljava/lang/String;)V",
   "gnu.mapping.SimpleSymbol.<init>()V",
   "com.google.appinventor.components.runtime.util.AlignmentUtil.<init>(Lcom/google/appinventor/components/runtime/LinearLayout;)V",
   "gnu.text.FilePath.hashCode()I",
   "gnu.lists.AbstractFormat.format(Ljava/lang/Object;Ljava/lang/StringBuffer;Ljava/text/FieldPosition;)Ljava/lang/StringBuffer;",
   "gnu.math.BaseUnit.<init>(Ljava/lang/String;Ljava/lang/String;)V",
   "gnu.bytecode.Type.hashCode()I",
   "gnu.expr.ClassExp.<init>()V",
   "gnu.kawa.reflect.FieldLocation.<init>(Ljava/lang/Object;Lgnu/bytecode/ClassType;Ljava/lang/String;)V",
   "gnu.math.Duration.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil$4.<init>(Lcom/google/appinventor/components/runtime/util/FullScreenVideoUtil;)V",
   "gnu.math.BaseUnit.<init>(Ljava/lang/String;)V",
   "kawa.lib.lists.<clinit>()V",
   "gnu.expr.ApplyExp.<init>(Lgnu/mapping/Procedure;[Lgnu/expr/Expression;)V",
   "gnu.text.RomanIntegerFormat.<init>(Z)V",
   "gnu.math.IntNum.<init>()V",
   "gnu.kawa.util.WeakIdentityHashMap.remove(Ljava/lang/Object;)Ljava/lang/Object;",
   "gnu.lists.LList.toString()Ljava/lang/String;",
   "gnu.mapping.WrongArguments.getMessage()Ljava/lang/String;",
   "gnu.mapping.Values.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen11.<clinit>()V",
   "kawa.lang.SyntaxForms$PairSyntaxForm.toString()Ljava/lang/String;",
   "gnu.text.FilePath.equals(Ljava/lang/Object;)Z",
   "gnu.mapping.WrongType.<init>(Lgnu/mapping/Procedure;ILjava/lang/ClassCastException;)V",
   "gnu.kawa.functions.LispEscapeFormat.<init>(III)V",
   "gnu.lists.ConsumerWriter.write([CII)V",
   "gnu.bytecode.Variable.<init>(Ljava/lang/String;Lgnu/bytecode/Type;)V",
   "gnu.text.LiteralFormat.<init>(Ljava/lang/String;)V",
   "gnu.kawa.functions.LispIterationFormat.<init>()V",
   "gnu.mapping.LogWriter.close()V",
   "gnu.mapping.CharArrayOutPort.<init>()V",
   "gnu.kawa.util.AbstractHashTable.get(Ljava/lang/Object;)Ljava/lang/Object;",
   "appinventor.ai_ariseo.OFWapp.Screen11.run()V",
   "gnu.mapping.Symbol.equals(Ljava/lang/Object;)Z",
   "gnu.kawa.functions.LispEscapeFormat.<init>(II)V",
   "gnu.math.Dimensions.<init>(Lgnu/math/BaseUnit;)V",
   "gnu.lists.FString.<init>(Lgnu/lists/CharSeq;II)V",
   "gnu.mapping.OutPort.<init>(Lgnu/mapping/OutPort;Z)V",
   "gnu.mapping.ValueStack.<init>()V",
   "gnu.mapping.InPort.<init>(Ljava/io/Reader;Lgnu/text/Path;)V",
   "gnu.lists.PairWithPosition.<init>()V",
   "gnu.mapping.Environment$InheritedLocal.<init>()V",
   "gnu.bytecode.Type$ClassToTypeMap.<init>()V",
   "gnu.expr.ErrorExp.<init>(Ljava/lang/String;)V",
   "gnu.mapping.LogWriter.write(Ljava/lang/String;II)V",
   "gnu.lists.TreePosition.<init>()V",
   "gnu.kawa.xml.ElementType.toString()Ljava/lang/String;",
   "gnu.expr.LambdaExp.<clinit>()V",
   "gnu.mapping.UnboundLocationException.<init>(Ljava/lang/Object;Ljava/lang/String;II)V",
   "gnu.bytecode.Field.toString()Ljava/lang/String;",
   "gnu.mapping.UnboundLocationException.<init>(Lgnu/mapping/Location;)V",
   "com.google.appinventor.components.runtime.Form.<init>()V",
   "kawa.lang.Promise.<init>(Lgnu/mapping/Procedure;)V",
   "gnu.math.RatNum.<clinit>()V",
   "gnu.mapping.ThreadLocation.<init>(Lgnu/mapping/Symbol;)V",
   "gnu.math.IntNum.doubleValue()D",
   "com.google.appinventor.components.runtime.collect.Sets.<init>()V",
   "gnu.lists.PrintConsumer.<init>(Lgnu/lists/Consumer;Z)V",
   "gnu.math.CComplex.<init>(Lgnu/math/RealNum;Lgnu/math/RealNum;)V",
   "gnu.text.Char.<clinit>()V",
   "kawa.lang.Syntax.<init>()V",
   "com.google.appinventor.components.runtime.LinearLayout$1.<init>(Lcom/google/appinventor/components/runtime/LinearLayout;Landroid/content/Context;Ljava/lang/Integer;Ljava/lang/Integer;)V",
   "gnu.kawa.functions.LispPluralFormat.<init>()V",
   "gnu.math.RatNum.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen9$frame.<init>()V",
   "gnu.mapping.WrongType.<init>(Ljava/lang/String;ILjava/lang/String;)V",
   "gnu.expr.BuiltinEnvironment.<clinit>()V",
   "com.google.appinventor.components.runtime.Form$4.<init>(Lcom/google/appinventor/components/runtime/Form;)V",
   "com.google.appinventor.components.runtime.EventDispatcher$EventClosure.hashCode()I",
   "gnu.text.EnglishIntegerFormat.parse(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Number;",
   "gnu.mapping.SimpleSymbol.<init>(Ljava/lang/String;)V",
   "gnu.text.URLPath.<init>(Ljava/net/URL;)V",
   "gnu.expr.ModuleContext$ClassToInstanceMap.<init>()V",
   "gnu.expr.BuiltinEnvironment.<init>()V",
   "gnu.bytecode.Variable.<init>(Ljava/lang/String;)V",
   "gnu.lists.FString.<init>(Lgnu/lists/CharSeq;)V",
   "gnu.math.DFloNum.equals(Ljava/lang/Object;)Z",
   "gnu.math.DFloNum.doubleValue()D",
   "gnu.lists.TreeList.hashCode()I",
   "gnu.expr.ScopeExp.toString()Ljava/lang/String;",
   "gnu.text.CompoundFormat.<init>([Ljava/text/Format;I)V",
   "gnu.mapping.Symbol.<clinit>()V",
   "gnu.expr.Compilation.<init>(Lgnu/expr/Language;Lgnu/text/SourceMessages;Lgnu/expr/NameLookup;)V",
   "gnu.math.Quantity.doubleValue()D",
   "com.google.appinventor.components.runtime.VideoPlayer.onPrepared(Landroid/media/MediaPlayer;)V",
   "gnu.mapping.InPort.<init>(Ljava/io/InputStream;)V",
   "gnu.mapping.WrappedException.<init>()V",
   "gnu.math.Quantity.<init>()V",
   "com.google.appinventor.components.runtime.VisibleComponent.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen6.<init>()V",
   "gnu.lists.FString.<init>(Lgnu/lists/Sequence;)V",
   "com.google.appinventor.components.runtime.util.JsonUtil.<init>()V",
   "gnu.mapping.CharArrayInPort.<init>([CI)V",
   "gnu.mapping.OutPort.<init>(Ljava/io/Writer;ZZ)V",
   "appinventor.ai_ariseo.OFWapp.Screen9.<clinit>()V",
   "gnu.mapping.SimpleEnvironment.<init>(I)V",
   "gnu.mapping.SimpleEnvironment.<init>()V",
   "gnu.mapping.InheritingEnvironment.<init>(Ljava/lang/String;Lgnu/mapping/Environment;)V",
   "gnu.mapping.LazyPropertyKey.<init>(Ljava/lang/String;)V",
   "gnu.mapping.OutPort.<init>(Ljava/io/Writer;Lgnu/text/PrettyWriter;Z)V",
   "gnu.text.WriterManager.<clinit>()V",
   "gnu.text.URIPath.toString()Ljava/lang/String;",
   "gnu.text.SourceError.<init>(CLjava/lang/String;IILjava/lang/String;)V",
   "gnu.text.ReportFormat.format(Ljava/lang/Object;Ljava/lang/StringBuffer;Ljava/text/FieldPosition;)Ljava/lang/StringBuffer;",
   "appinventor.ai_ariseo.OFWapp.Screen7.run()V",
   "gnu.expr.ErrorExp.<init>(Ljava/lang/String;Lgnu/text/SourceMessages;)V",
   "gnu.kawa.util.HashNode.hashCode()I",
   "gnu.math.IntFraction.<init>()V",
   "com.google.appinventor.components.runtime.util.MediaUtil$MediaSource.<init>(Ljava/lang/String;I)V",
   "gnu.mapping.OutPort.<init>(Ljava/io/OutputStream;)V",
   "gnu.bytecode.PrimType.<init>(Ljava/lang/String;Ljava/lang/String;ILjava/lang/Class;)V",
   "gnu.text.CompoundFormat.parseObject(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Object;",
   "gnu.lists.GeneralArray.toString()Ljava/lang/String;",
   "gnu.kawa.functions.LispFormat.<init>([C)V",
   "gnu.mapping.WrappedException.toString()Ljava/lang/String;",
   "gnu.lists.LList.<init>()V",
   "gnu.mapping.WrongType.<init>(Lgnu/mapping/Procedure;ILjava/lang/Object;)V",
   "gnu.math.DateTime.<clinit>()V",
   "com.google.appinventor.components.runtime.EventDispatcher$EventClosure.<init>(Ljava/lang/String;Ljava/lang/String;Lcom/google/appinventor/components/runtime/EventDispatcher$1;)V",
   "gnu.kawa.util.AbstractHashTable.<init>(I)V",
   "gnu.text.LiteralFormat.<init>(Ljava/lang/StringBuffer;)V",
   "gnu.mapping.CallContext.<clinit>()V",
   "com.google.appinventor.components.runtime.errors.YailRuntimeError.<init>(Ljava/lang/String;Ljava/lang/String;)V",
   "gnu.mapping.ThreadLocation.<init>(Lgnu/mapping/Symbol;Ljava/lang/Object;Lgnu/mapping/SharedLocation;)V",
   "gnu.mapping.OutPort.<init>(Ljava/io/Writer;)V",
   "kawa.lib.numbers.<clinit>()V",
   "gnu.expr.Language.<init>()V",
   "gnu.bytecode.Location.<init>()V",
   "kawa.lang.AutoloadSyntax.toString()Ljava/lang/String;",
   "gnu.mapping.Procedure2.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen7.<init>()V",
   "gnu.math.Numeric.floatValue()F",
   "gnu.lists.TreeList.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen6.run()V",
   "com.google.appinventor.components.runtime.VideoPlayer$ResizableVideoView.<init>(Lcom/google/appinventor/components/runtime/VideoPlayer;Landroid/content/Context;)V",
   "gnu.text.LineBufferedReader.close()V",
   "gnu.expr.Declaration.<init>(Ljava/lang/Object;Lgnu/bytecode/Field;)V",
   "gnu.mapping.LogWriter.flush()V",
   "gnu.expr.LambdaExp.<init>()V",
   "gnu.lists.AbstractSequence.hashCode()I",
   "gnu.lists.ImmutablePair.<init>()V",
   "com.google.appinventor.components.runtime.util.YailNumberToString.<init>()V",
   "gnu.math.DQuantity.doubleValue()D",
   "gnu.kawa.functions.LispRealFormat.<init>()V",
   "gnu.kawa.util.AbstractHashTable.remove(Ljava/lang/Object;)Ljava/lang/Object;",
   "gnu.math.Unit.hashCode()I",
   "gnu.mapping.InPort.<clinit>()V",
   "gnu.kawa.xml.NodeType.<init>(Ljava/lang/String;)V",
   "gnu.expr.Compilation.toString()Ljava/lang/String;",
   "gnu.text.LineInputStreamReader.ready()Z",
   "gnu.mapping.Symbol.toString()Ljava/lang/String;",
   "gnu.text.LineBufferedReader.ready()Z",
   "appinventor.ai_ariseo.OFWapp.Screen6$frame.<init>()V",
   "gnu.expr.Declaration.<init>()V",
   "com.google.appinventor.components.runtime.EventDispatcher$EventClosure.equals(Ljava/lang/Object;)Z",
   "gnu.lists.TreeList.<init>(Lgnu/lists/TreeList;)V",
   "gnu.kawa.reflect.SlotGet.<init>(Ljava/lang/String;ZLgnu/mapping/Procedure;)V",
   "gnu.mapping.OutPort.print(J)V",
   "com.google.appinventor.components.runtime.VideoPlayer$ResizableVideoView.onMeasure(II)V",
   "gnu.math.Duration.equals(Ljava/lang/Object;)Z",
   "gnu.xml.XMLPrinter.<init>(Ljava/io/OutputStream;Z)V",
   "gnu.math.DComplex.<init>(DD)V",
   "gnu.kawa.util.AbstractHashTable.clear()V",
   "kawa.lang.SyntaxRules.<init>([Ljava/lang/Object;[Lkawa/lang/SyntaxRule;I)V",
   "kawa.lang.AutoloadSyntax.<init>(Ljava/lang/String;Ljava/lang/String;)V",
   "gnu.lists.PairWithPosition.<init>(Ljava/lang/Object;Ljava/lang/Object;)V",
   "gnu.kawa.functions.Arithmetic.<init>()V",
   "com.google.appinventor.components.runtime.util.MediaUtil.<clinit>()V",
   "gnu.lists.FVector.equals(Ljava/lang/Object;)Z",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil$2.onStop()V",
   "kawa.lang.SyntaxRule.<init>(Lkawa/lang/SyntaxPattern;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;I)V",
   "gnu.mapping.ThreadLocation.<init>()V",
   "gnu.lists.SeqPosition.<init>(Lgnu/lists/AbstractSequence;IZ)V",
   "gnu.lists.Pair.equals(Ljava/lang/Object;)Z",
   "gnu.text.LineBufferedReader.read()I",
   "gnu.bytecode.Field.<init>(Lgnu/bytecode/ClassType;)V",
   "gnu.expr.QuoteExp.<init>(Ljava/lang/Object;)V",
   "gnu.mapping.WrongType.getMessage()Ljava/lang/String;",
   "gnu.lists.FString.hashCode()I",
   "gnu.kawa.xml.KNode.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.ListPickerActivity.onKeyDown(ILandroid/view/KeyEvent;)Z",
   "com.google.appinventor.components.runtime.EventDispatcher$EventRegistry.<init>(Lcom/google/appinventor/components/runtime/HandlesEventDispatching;)V",
   "gnu.mapping.WrappedException.<init>(Ljava/lang/Throwable;)V",
   "gnu.math.BaseUnit.hashCode()I",
   "kawa.lang.AutoloadSyntax.<init>()V",
   "gnu.mapping.UnboundLocationException.<init>(Ljava/lang/Object;Ljava/lang/String;)V",
   "gnu.kawa.reflect.OccurrenceType.toString()Ljava/lang/String;",
   "gnu.kawa.functions.IntegerFormat.<init>()V",
   "gnu.text.SourceError.toString()Ljava/lang/String;",
   "gnu.lists.FString.<init>(Ljava/lang/StringBuffer;II)V",
   "gnu.text.FilePath.toString()Ljava/lang/String;",
   "gnu.math.IntNum.<clinit>()V",
   "appinventor.ai_ariseo.OFWapp.Screen8.<init>()V",
   "gnu.text.URIPath.equals(Ljava/lang/Object;)Z",
   "gnu.mapping.Procedure.toString()Ljava/lang/String;",
   "gnu.kawa.functions.LispRepositionFormat.<init>(IZZ)V",
   "gnu.math.IntFraction.doubleValue()D",
   "kawa.lib.numbers.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen4$frame.<init>()V",
   "gnu.math.RatNum.equals(Ljava/lang/Object;)Z",
   "gnu.kawa.reflect.ClassMemberLocation.<init>(Ljava/lang/Object;Lgnu/bytecode/ClassType;Ljava/lang/String;)V",
   "gnu.lists.SeqPosition.<init>(Lgnu/lists/AbstractSequence;)V",
   "gnu.mapping.CharArrayInPort.<init>([C)V",
   "gnu.mapping.WrongType.<init>(Ljava/lang/ClassCastException;Ljava/lang/String;ILjava/lang/Object;)V",
   "gnu.mapping.WrongType.<init>(Ljava/lang/String;ILjava/lang/Object;Ljava/lang/String;)V",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil$2.onStart()V",
   "gnu.lists.SeqPosition.<init>(Lgnu/lists/AbstractSequence;I)V",
   "gnu.mapping.NamedLocation.<init>(Lgnu/mapping/NamedLocation;)V",
   "gnu.kawa.functions.ParseFormat.<init>(Z)V",
   "gnu.kawa.util.AbstractHashTable.entrySet()Ljava/util/Set;",
   "gnu.text.Lexer.close()V",
   "gnu.text.PrettyWriter.<clinit>()V",
   "com.google.appinventor.components.runtime.errors.RuntimeError.<init>(Ljava/lang/String;)V",
   "gnu.text.FlushFormat.<init>()V",
   "gnu.math.Complex.equals(Ljava/lang/Object;)Z",
   "gnu.bytecode.Method.<init>(Lgnu/bytecode/ClassType;I)V",
   "gnu.expr.KawaConvert.<clinit>()V",
   "gnu.expr.Expression.<clinit>()V",
   "gnu.lists.TreePosition.<init>(Lgnu/lists/TreePosition;)V",
   "gnu.kawa.reflect.ClassMemberLocation.<init>(Ljava/lang/Object;Ljava/lang/reflect/Field;)V",
   "gnu.xml.XMLPrinter.<init>(Ljava/io/Writer;)V",
   "gnu.kawa.xml.TreeScanner.toString()Ljava/lang/String;",
   "gnu.lists.PrintConsumer.<init>(Ljava/io/OutputStream;Z)V",
   "com.google.appinventor.components.runtime.util.SmsBroadcastReceiver.onReceive(Landroid/content/Context;Landroid/content/Intent;)V",
   "gnu.lists.TreePosition.<init>(Ljava/lang/Object;)V",
   "gnu.math.DFloNum.<clinit>()V",
   "kawa.lang.SyntaxTemplate.<init>()V",
   "gnu.math.BaseUnit.<clinit>()V",
   "kawa.lib.lists.<init>()V",
   "gnu.expr.PrimProcedure.<init>(Ljava/lang/reflect/Method;Lgnu/expr/Language;)V",
   "com.google.appinventor.components.runtime.Form.onActivityResult(IILandroid/content/Intent;)V",
   "kawa.lang.SyntaxRules.<init>([Ljava/lang/Object;Ljava/lang/Object;Lkawa/lang/Translator;)V",
   "gnu.expr.Expression.<init>()V",
   "gnu.text.Options.<init>()V",
   "gnu.expr.LetExp.<init>([Lgnu/expr/Expression;)V",
   "kawa.standard.require.<init>()V",
   "gnu.kawa.util.AbstractHashTable.size()I",
   "gnu.text.EnglishIntegerFormat.format(DLjava/lang/StringBuffer;Ljava/text/FieldPosition;)Ljava/lang/StringBuffer;",
   "gnu.mapping.UnboundLocationException.<init>(Ljava/lang/Object;)V",
   "gnu.lists.ConsumerWriter.close()V",
   "gnu.kawa.reflect.StaticFieldLocation.<init>(Ljava/lang/String;Ljava/lang/String;)V",
   "gnu.text.EnglishIntegerFormat.<clinit>()V",
   "gnu.mapping.Symbol.<init>()V",
   "gnu.expr.ModuleMethod.<init>(Lgnu/expr/ModuleBody;ILjava/lang/Object;ILjava/lang/Object;)V",
   "gnu.text.FilePath.<init>(Ljava/io/File;Ljava/lang/String;)V",
   "gnu.kawa.reflect.StaticFieldLocation.<init>(Ljava/lang/reflect/Field;)V",
   "gnu.mapping.MethodProc.<init>()V",
   "gnu.text.CaseConvertFormat.<init>(Ljava/text/Format;C)V",
   "gnu.expr.LambdaExp.toString()Ljava/lang/String;",
   "gnu.math.ExponentialFormat.format(Ljava/lang/Object;Ljava/lang/StringBuffer;Ljava/text/FieldPosition;)Ljava/lang/StringBuffer;",
   "gnu.bytecode.Type.<clinit>()V",
   "gnu.kawa.util.WeakIdentityHashMap.<init>()V",
   "kawa.lib.strings.<init>()V",
   "gnu.lists.SubSequence.finalize()V",
   "gnu.lists.FString.<init>([C)V",
   "gnu.mapping.LogWriter.write([CII)V",
   "gnu.lists.GeneralArray.<init>([I)V",
   "gnu.bytecode.ArrayType.<init>(Lgnu/bytecode/Type;)V",
   "appinventor.ai_ariseo.OFWapp.Screen5.<init>()V",
   "gnu.expr.ClassExp.<init>(Z)V",
   "gnu.mapping.Procedure.<clinit>()V",
   "gnu.lists.FVector.<init>(ILjava/lang/Object;)V",
   "gnu.mapping.InPort.<init>(Ljava/io/Reader;)V",
   "gnu.kawa.xml.ProcessingInstructionType.toString()Ljava/lang/String;",
   "gnu.bytecode.ClassType.<init>()V",
   "kawa.lang.SyntaxPattern.<init>(Ljava/lang/StringBuffer;Ljava/lang/Object;Lkawa/lang/SyntaxForm;[Ljava/lang/Object;Lkawa/lang/Translator;)V",
   "gnu.math.DFloNum.<init>(D)V",
   "com.google.appinventor.components.runtime.ListPickerActivity.<init>()V",
   "com.google.appinventor.components.runtime.Form.onPrepareDialog(ILandroid/app/Dialog;)V",
   "gnu.expr.ModuleMethod.<init>(Lgnu/expr/ModuleBody;ILjava/lang/Object;I)V",
   "gnu.math.MulUnit.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil.<init>(Lcom/google/appinventor/components/runtime/Form;Landroid/os/Handler;)V",
   "gnu.mapping.WrongType.<init>(Ljava/lang/ClassCastException;Lgnu/mapping/Procedure;ILjava/lang/Object;)V",
   "com.google.appinventor.components.runtime.Form.<clinit>()V",
   "com.google.appinventor.components.runtime.errors.RuntimeError.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen5$frame.<init>()V",
   "com.google.appinventor.components.runtime.VideoPlayer.onCompletion(Landroid/media/MediaPlayer;)V",
   "kawa.lib.kawa.hashtable$HashTable.<init>(Lgnu/mapping/Procedure;Lgnu/mapping/Procedure;)V",
   "gnu.lists.SimpleVector.<init>()V",
   "com.google.appinventor.components.runtime.util.MediaUtil$FlushedInputStream.skip(J)J",
   "gnu.kawa.reflect.FieldLocation.<init>(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V",
   "gnu.kawa.util.AbstractWeakHashTable.<init>(I)V",
   "gnu.lists.TreePosition.<init>(Lgnu/lists/AbstractSequence;I)V",
   "gnu.mapping.WrongType.<init>(Lgnu/mapping/Procedure;ILjava/lang/Object;Lgnu/bytecode/Type;)V",
   "gnu.expr.PrimProcedure.<init>(ILgnu/bytecode/Type;[Lgnu/bytecode/Type;)V",
   "gnu.mapping.OutPort.print(Z)V",
   "com.google.appinventor.components.runtime.util.MediaUtil$FlushedInputStream.<init>(Ljava/io/InputStream;)V",
   "gnu.mapping.NamedLocation.<init>(Lgnu/mapping/Symbol;Ljava/lang/Object;)V",
   "gnu.lists.AbstractSequence.<init>()V",
   "gnu.kawa.functions.LispTabulateFormat.<init>(IIIZ)V",
   "gnu.text.Char.hashCode()I",
   "appinventor.ai_ariseo.OFWapp.Screen8.<clinit>()V",
   "gnu.kawa.functions.LispNewlineFormat.<init>()V",
   "gnu.expr.PrimProcedure.<init>(Lgnu/bytecode/Method;)V",
   "gnu.math.DComplex.toString()Ljava/lang/String;",
   "gnu.math.Dimensions.<init>()V",
   "gnu.text.Options$OptionInfo.<init>()V",
   "gnu.kawa.functions.ParseFormat.<clinit>()V",
   "appinventor.ai_ariseo.OFWapp.Screen7.<clinit>()V",
   "gnu.bytecode.Type.<init>()V",
   "gnu.text.PrettyWriter.write(I)V",
   "kawa.lang.Macro.<init>(Ljava/lang/Object;Lgnu/mapping/Procedure;)V",
   "gnu.expr.Declaration.<init>(Lgnu/bytecode/Variable;)V",
   "com.google.appinventor.components.runtime.Form.onResume()V",
   "gnu.kawa.functions.LispFormat.<init>(Ljava/lang/String;)V",
   "kawa.lib.ports.<init>()V",
   "gnu.expr.Keyword.<init>(Ljava/lang/String;)V",
   "gnu.text.LineInputStreamReader.close()V",
   "appinventor.ai_ariseo.OFWapp.Screen5.run()V",
   "gnu.expr.Keyword.<init>()V",
   "gnu.mapping.Location.<init>()V",
   "gnu.text.Lexer.<init>(Lgnu/text/LineBufferedReader;Lgnu/text/SourceMessages;)V",
   "gnu.mapping.Values.<clinit>()V",
   "kawa.lib.kawa.hashtable$HashTable.<init>(Lgnu/mapping/Procedure;Lgnu/mapping/Procedure;I)V",
   "gnu.lists.ImmutablePair.<init>(Ljava/lang/Object;Ljava/lang/Object;)V",
   "com.google.appinventor.components.runtime.Form$2.<init>(Lcom/google/appinventor/components/runtime/Form;)V",
   "gnu.mapping.Environment.<clinit>()V",
   "gnu.mapping.Symbol.<init>(Lgnu/mapping/Namespace;Ljava/lang/String;)V",
   "gnu.mapping.PlainLocation.<init>(Lgnu/mapping/Symbol;Ljava/lang/Object;)V",
   "gnu.expr.QuoteExp.toString()Ljava/lang/String;",
   "gnu.math.FixedRealFormat.parseObject(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Object;",
   "appinventor.ai_ariseo.OFWapp.Screen9.run()V",
   "gnu.kawa.functions.LispFormat.<init>([CII)V",
   "com.google.appinventor.components.runtime.Form$4.onMenuItemClick(Landroid/view/MenuItem;)Z",
   "gnu.math.Dimensions.<init>(Lgnu/math/Dimensions;ILgnu/math/Dimensions;II)V",
   "gnu.expr.QuoteExp.<clinit>()V",
   "kawa.lang.Macro.toString()Ljava/lang/String;",
   "gnu.lists.LList.equals(Ljava/lang/Object;)Z",
   "gnu.mapping.UnboundLocationException.getMessage()Ljava/lang/String;",
   "gnu.expr.PrimProcedure.<init>(Lgnu/bytecode/Method;Lgnu/expr/LambdaExp;)V",
   "gnu.expr.ScopeExp.<init>()V",
   "gnu.lists.FVector.<init>([Ljava/lang/Object;)V",
   "gnu.expr.ModuleManager.<clinit>()V",
   "com.google.youngandroid.runtime$frame.<init>()V",
   "gnu.math.DFloNum.longValue()J",
   "gnu.text.PrettyWriter.close()V",
   "kawa.lang.Macro.<init>(Lkawa/lang/Macro;)V",
   "gnu.lists.GeneralArray.<clinit>()V",
   "com.google.appinventor.components.runtime.VideoPlayer.<init>(Lcom/google/appinventor/components/runtime/ComponentContainer;)V",
   "gnu.kawa.util.AbstractWeakHashTable.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
   "gnu.lists.Convert.<init>()V",
   "gnu.lists.ConsumerWriter.finalize()V",
   "gnu.lists.Pair.<init>(Ljava/lang/Object;Ljava/lang/Object;)V",
   "com.google.appinventor.components.runtime.util.MediaUtil.<init>()V",
   "gnu.mapping.OutPort.print(D)V",
   "gnu.expr.SetExp.toString()Ljava/lang/String;",
   "gnu.text.PrettyWriter.write(Ljava/lang/String;II)V",
   "gnu.math.IntNum.longValue()J",
   "gnu.expr.Compilation.<clinit>()V",
   "com.google.appinventor.components.runtime.EventDispatcher.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen1.<clinit>()V",
   "gnu.expr.Expression.toString()Ljava/lang/String;",
   "gnu.text.WriterManager.<init>()V",
   "gnu.kawa.util.AbstractWeakHashTable.<init>()V",
   "gnu.text.LineBufferedReader.mark(I)V",
   "gnu.bytecode.Type.toString()Ljava/lang/String;",
   "gnu.text.WriterRef.<init>(Ljava/io/Writer;)V",
   "gnu.kawa.util.GeneralHashTable.<init>()V",
   "kawa.lang.Pattern.<init>()V",
   "gnu.math.DateTime.<init>(I)V",
   "gnu.math.Numeric.toString()Ljava/lang/String;",
   "gnu.text.IntegerFormat.<init>()V",
   "com.google.appinventor.components.runtime.WebViewActivity$1.shouldOverrideUrlLoading(Landroid/webkit/WebView;Ljava/lang/String;)Z",
   "gnu.text.PrettyWriter.write([C)V",
   "kawa.lang.Pattern.<clinit>()V",
   "gnu.lists.GeneralArray.<init>()V",
   "gnu.math.DFloNum.hashCode()I",
   "kawa.lang.SyntaxTemplate.<init>(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;I)V",
   "gnu.kawa.functions.LispPrettyFormat.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.Form.onCreate(Landroid/os/Bundle;)V",
   "com.google.appinventor.components.runtime.util.SmsBroadcastReceiver.<init>()V",
   "gnu.expr.PrimProcedure.<init>(Ljava/lang/String;Ljava/lang/String;I)V",
   "appinventor.ai_ariseo.OFWapp.Screen5.<clinit>()V",
   "com.google.appinventor.components.runtime.AndroidViewComponent.<init>(Lcom/google/appinventor/components/runtime/ComponentContainer;)V",
   "gnu.bytecode.ObjectType.<init>(Ljava/lang/String;)V",
   "kawa.lang.SyntaxRule.<init>(Lkawa/lang/SyntaxPattern;Ljava/lang/Object;Lkawa/lang/SyntaxForm;Lkawa/lang/Translator;)V",
   "gnu.lists.Sequence.<clinit>()V",
   "gnu.xml.XMLPrinter.<init>(Ljava/io/OutputStream;Lgnu/text/Path;)V",
   "com.google.appinventor.components.runtime.util.MediaUtil$MediaSource.<clinit>()V",
   "gnu.expr.QuoteExp.<init>(Ljava/lang/Object;Lgnu/bytecode/Type;)V",
   "gnu.math.Unit.<init>()V",
   "gnu.mapping.WrongType.<init>(ILjava/lang/Object;Lgnu/bytecode/Type;)V",
   "gnu.mapping.WrongArguments.<init>(Lgnu/mapping/Procedure;I)V",
   "gnu.math.DComplex.<init>()V",
   "gnu.text.EnglishIntegerFormat.format(JLjava/lang/StringBuffer;Ljava/text/FieldPosition;)Ljava/lang/StringBuffer;",
   "gnu.kawa.reflect.StaticFieldLocation.<init>(Lgnu/bytecode/ClassType;Ljava/lang/String;)V",
   "gnu.text.ReportFormat.<init>()V",
   "gnu.expr.Keyword.<init>(Lgnu/mapping/Namespace;Ljava/lang/String;)V",
   "gnu.lists.FString.<init>(Ljava/lang/String;)V",
   "gnu.text.PrettyWriter.<init>(Ljava/io/Writer;Z)V",
   "gnu.mapping.ThreadLocation.<init>(Ljava/lang/String;)V",
   "appinventor.ai_ariseo.OFWapp.Screen10.<clinit>()V",
   "gnu.mapping.Symbol.hashCode()I",
   "gnu.kawa.reflect.FieldLocation.toString()Ljava/lang/String;",
   "gnu.kawa.lispexpr.LispReader.<init>(Lgnu/text/LineBufferedReader;Lgnu/text/SourceMessages;)V",
   "kawa.lib.kawa.hashtable$HashTable.<init>(Lkawa/lib/kawa/hashtable$HashTable;Z)V",
   "gnu.mapping.Location.toString()Ljava/lang/String;",
   "kawa.lang.SyntaxPattern.<init>(Ljava/lang/String;[Ljava/lang/Object;I)V",
   "gnu.xml.XMLPrinter.print(Ljava/lang/Object;)V",
   "gnu.math.Complex.<init>()V",
   "gnu.expr.PrimProcedure.<init>(Lgnu/bytecode/Method;Lgnu/expr/Language;)V",
   "gnu.lists.PrintConsumer.<init>(Ljava/io/Writer;)V",
   "gnu.math.FixedRealFormat.<init>()V",
   "gnu.kawa.functions.NumberCompare.<init>()V",
   "gnu.bytecode.Method.<init>()V",
   "gnu.kawa.xml.NodeType.<clinit>()V",
   "gnu.lists.Pair.hashCode()I",
   "gnu.expr.ApplyExp.toString()Ljava/lang/String;",
   "gnu.mapping.CharArrayInPort.read()I",
   "gnu.mapping.WrongType.<init>(Lgnu/mapping/Procedure;ILjava/lang/Object;Ljava/lang/String;)V",
   "gnu.mapping.Procedure0.<init>()V",
   "gnu.mapping.Environment.<init>()V",
   "gnu.mapping.Environment.toString()Ljava/lang/String;",
   "gnu.lists.FString.<init>()V",
   "gnu.bytecode.PrimType.<init>(Lgnu/bytecode/PrimType;)V",
   "gnu.mapping.SymbolRef.<init>(Lgnu/mapping/Symbol;Lgnu/mapping/Namespace;)V",
   "gnu.mapping.CallContext.<init>()V",
   "gnu.text.PrettyWriter.write([CII)V",
   "gnu.mapping.Namespace.<clinit>()V",
   "gnu.kawa.functions.ObjectFormat.<init>(Z)V",
   "gnu.xml.NodeTree.<init>()V",
   "gnu.mapping.IndirectableLocation.<clinit>()V",
   "gnu.xml.XMLPrinter.<init>(Lgnu/mapping/OutPort;Z)V",
   "gnu.expr.ModuleContext.<init>(Lgnu/expr/ModuleManager;)V",
   "gnu.mapping.SymbolRef.toString()Ljava/lang/String;",
   "gnu.expr.Declaration.<init>(Ljava/lang/Object;)V",
   "gnu.text.LineBufferedReader.reset()V",
   "gnu.mapping.ThreadLocation$InheritingLocation.<init>(Lgnu/mapping/ThreadLocation;)V",
   "gnu.expr.ReferenceExp.<init>(Lgnu/expr/Declaration;)V",
   "gnu.mapping.WrappedException.<init>(Ljava/lang/String;)V",
   "com.google.appinventor.components.runtime.Form$2.run()V",
   "com.google.appinventor.components.runtime.AndroidNonvisibleComponent.<init>(Lcom/google/appinventor/components/runtime/Form;)V",
   "gnu.text.LineBufferedReader.markSupported()Z",
   "kawa.lang.SyntaxForms.<init>()V",
   "gnu.text.PrettyWriter.write(Ljava/lang/String;)V",
   "gnu.xml.NodeTree.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.Form.onDestroy()V",
   "gnu.math.RealNum.<init>()V",
   "gnu.expr.Declaration.toString()Ljava/lang/String;",
   "kawa.lib.misc.<init>()V",
   "com.google.appinventor.components.runtime.Texting.<clinit>()V",
   "gnu.math.Unit.toString()Ljava/lang/String;",
   "gnu.text.ReportFormat.parseObject(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Object;",
   "gnu.text.CompoundFormat.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.util.SdkLevel.<init>()V",
   "gnu.text.Char.toString()Ljava/lang/String;",
   "gnu.expr.ReferenceExp.<init>(Ljava/lang/Object;Lgnu/expr/Declaration;)V",
   "gnu.mapping.Procedure.<init>(Ljava/lang/String;)V",
   "com.google.appinventor.components.runtime.Form.onStop()V",
   "gnu.expr.ReferenceExp.<init>(Ljava/lang/Object;)V",
   "gnu.mapping.LogWriter.<init>(Ljava/io/Writer;)V",
   "gnu.mapping.PropertySet.<init>()V",
   "kawa.lib.ports.<clinit>()V",
   "gnu.lists.ConsumerWriter.<init>(Lgnu/lists/Consumer;)V",
   "gnu.mapping.OutPort.print(F)V",
   "gnu.text.Lexer.read()I",
   "com.google.appinventor.components.runtime.EventDispatcher.<clinit>()V",
   "gnu.kawa.functions.LispObjectFormat.<init>(Lgnu/text/ReportFormat;IIIII)V",
   "gnu.text.SourceError.<init>(Lgnu/text/LineBufferedReader;CLjava/lang/String;)V",
   "gnu.math.IntFraction.longValue()J",
   "gnu.text.CharMap.<init>()V",
   "gnu.kawa.functions.Arithmetic.<clinit>()V",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil.onPrepared(Landroid/media/MediaPlayer;)V",
   "gnu.text.RomanIntegerFormat.<init>()V",
   "gnu.math.Dimensions.<clinit>()V",
   "kawa.lang.Translator.<init>(Lgnu/expr/Language;Lgnu/text/SourceMessages;Lgnu/expr/NameLookup;)V",
   "appinventor.ai_ariseo.OFWapp.Screen10.run()V",
   "gnu.text.EnglishIntegerFormat.<init>(Z)V",
   "gnu.text.Lexer.<init>(Lgnu/text/LineBufferedReader;)V",
   "gnu.expr.AccessExp.<init>()V",
   "gnu.mapping.WrappedException.<init>(Ljava/lang/String;Ljava/lang/Throwable;)V",
   "gnu.math.Numeric.intValue()I",
   "gnu.bytecode.ArrayType.<init>(Lgnu/bytecode/Type;Ljava/lang/String;)V",
   "gnu.math.ExponentialFormat.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen1.<init>()V",
   "kawa.lang.Translator.<clinit>()V",
   "gnu.lists.FString.<init>(Ljava/lang/StringBuffer;)V",
   "gnu.lists.FString.<clinit>()V",
   "gnu.text.Char.<init>(I)V",
   "gnu.mapping.OutPort.<init>(Ljava/io/OutputStream;Lgnu/text/Path;)V",
   "gnu.kawa.util.WeakIdentityHashMap.<init>(I)V",
   "appinventor.ai_ariseo.OFWapp.Screen9.<init>()V",
   "gnu.lists.Convert.<clinit>()V",
   "gnu.mapping.OutPort.<init>(Ljava/io/Writer;ZLgnu/text/Path;)V",
   "gnu.bytecode.UninitializedType.toString()Ljava/lang/String;",
   "gnu.mapping.OutPort.print(Ljava/lang/String;)V",
   "gnu.lists.SeqPosition.finalize()V",
   "gnu.bytecode.ObjectType.<init>()V",
   "com.google.appinventor.components.runtime.WebViewActivity.onCreate(Landroid/os/Bundle;)V",
   "gnu.bytecode.Method.<init>(Lgnu/bytecode/Method;Lgnu/bytecode/ClassType;)V",
   "gnu.mapping.CharArrayInPort.<init>(Ljava/lang/String;)V",
   "gnu.mapping.PlainLocation.<init>(Lgnu/mapping/Symbol;Ljava/lang/Object;Ljava/lang/Object;)V",
   "gnu.mapping.SimpleEnvironment.<init>(Ljava/lang/String;)V",
   "gnu.math.IntNum.equals(Ljava/lang/Object;)Z",
   "gnu.text.Path.<init>()V",
   "gnu.math.NamedUnit.<init>()V",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil.onCompletion(Landroid/media/MediaPlayer;)V",
   "gnu.expr.ModuleInfo.<init>()V",
   "gnu.expr.PrimProcedure.<clinit>()V",
   "gnu.mapping.Procedure.<init>()V",
   "gnu.kawa.functions.ObjectFormat.parseObject(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Object;",
   "gnu.lists.FVector.<init>(Ljava/util/List;)V",
   "com.google.youngandroid.runtime$frame2.<init>()V",
   "gnu.text.RomanIntegerFormat.format(JLjava/lang/StringBuffer;Ljava/text/FieldPosition;)Ljava/lang/StringBuffer;",
   "gnu.kawa.xml.AttributeType.toString()Ljava/lang/String;",
   "gnu.math.Numeric.longValue()J",
   "gnu.text.SourceError.<init>(CLgnu/text/SourceLocator;Ljava/lang/String;)V",
   "gnu.kawa.util.HashNode.equals(Ljava/lang/Object;)Z",
   "gnu.mapping.Procedure1.<init>()V",
   "gnu.expr.ModuleBody.run()V",
   "gnu.math.BaseUnit.<init>()V",
   "gnu.lists.SeqPosition.<init>()V",
   "gnu.mapping.Location.<clinit>()V",
   "gnu.mapping.Procedure2.<init>(Ljava/lang/String;)V",
   "gnu.bytecode.Method.toString()Ljava/lang/String;",
   "gnu.lists.AbstractSequence.equals(Ljava/lang/Object;)Z",
   "com.google.appinventor.components.runtime.Form.onKeyDown(ILandroid/view/KeyEvent;)Z",
   "kawa.lang.Macro.<init>()V",
   "com.google.youngandroid.runtime.<init>()V",
   "gnu.expr.NameLookup.<init>(Lgnu/expr/Language;)V",
   "gnu.mapping.Values.<init>([Ljava/lang/Object;)V",
   "gnu.expr.ModuleBody.<init>()V",
   "com.google.appinventor.components.runtime.Form$1.run()V",
   "com.google.appinventor.components.runtime.LinearLayout.<init>(Landroid/content/Context;ILjava/lang/Integer;Ljava/lang/Integer;)V",
   "gnu.math.Duration.hashCode()I",
   "kawa.standard.require.<clinit>()V",
   "gnu.mapping.ProcedureN.<init>(Ljava/lang/String;)V",
   "gnu.lists.SeqPosition.toString()Ljava/lang/String;",
   "com.google.appinventor.components.runtime.Form.onConfigurationChanged(Landroid/content/res/Configuration;)V",
   "gnu.kawa.functions.LispPrettyFormat.<init>()V",
   "gnu.expr.Declaration.<init>(Ljava/lang/Object;Lgnu/bytecode/Type;)V",
   "gnu.bytecode.ClassType.toString()Ljava/lang/String;",
   "gnu.xml.XMLPrinter.write([CII)V",
   "gnu.math.Unit.doubleValue()D",
   "gnu.mapping.Namespace.<init>(I)V",
   "gnu.lists.AbstractFormat.<init>()V",
   "gnu.text.Options.<init>(Lgnu/text/Options;)V",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil$1.onBackPressed()V",
   "gnu.mapping.LogWriter.write(I)V",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil$4.run()V",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil$1.onStart()V",
   "gnu.bytecode.Variable.<init>()V",
   "com.google.appinventor.components.runtime.Texting.<init>(Lcom/google/appinventor/components/runtime/ComponentContainer;)V",
   "appinventor.ai_ariseo.OFWapp.Screen10.<init>()V",
   "gnu.math.DateTime.<init>(ILjava/util/GregorianCalendar;)V",
   "gnu.expr.ReferenceExp.toString()Ljava/lang/String;",
   "gnu.kawa.reflect.SlotGet.<init>(Ljava/lang/String;Z)V",
   "gnu.kawa.reflect.ClassMemberLocation.<init>(Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;)V",
   "appinventor.ai_ariseo.OFWapp.Screen1$frame.<init>()V",
   "gnu.kawa.functions.LispFreshlineFormat.<init>(I)V",
   "kawa.lang.PatternScope.<init>()V",
   "com.google.appinventor.components.runtime.Form$1.<init>(Lcom/google/appinventor/components/runtime/Form;I)V",
   "gnu.mapping.Namespace.toString()Ljava/lang/String;",
   "gnu.lists.FString.<init>(Ljava/lang/CharSequence;)V",
   "gnu.text.PrettyWriter.<init>(Ljava/io/Writer;I)V",
   "gnu.math.CComplex.<init>()V",
   "gnu.mapping.UnboundLocationException.<init>(Ljava/lang/Object;Lgnu/text/SourceLocator;)V",
   "kawa.lang.Macro.<init>(Ljava/lang/Object;)V",
   "gnu.expr.ModuleContext.<clinit>()V",
   "gnu.kawa.util.AbstractWeakHashTable$WEntry.<init>(Ljava/lang/Object;Lgnu/kawa/util/AbstractWeakHashTable;I)V",
   "com.google.appinventor.components.runtime.util.AnimationUtil.<init>()V",
   "gnu.mapping.MethodProc.<clinit>()V",
   "gnu.expr.PrimProcedure.toString()Ljava/lang/String;",
   "gnu.kawa.functions.Format.<init>()V",
   "com.google.appinventor.components.runtime.WebViewActivity.<init>()V",
   "gnu.text.URIPath.<init>(Ljava/net/URI;)V",
   "gnu.expr.ModuleInfo$ClassToInfoMap.<init>()V",
   "gnu.math.DFloNum.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen8$frame.<init>()V",
   "gnu.bytecode.ClassType.<init>(Ljava/lang/String;)V",
   "gnu.text.SourceMessages.<init>()V",
   "gnu.mapping.OutPort.close()V",
   "gnu.expr.Language.<clinit>()V",
   "gnu.expr.NameLookup.<clinit>()V",
   "gnu.mapping.InPort.<init>(Ljava/io/InputStream;Lgnu/text/Path;Ljava/lang/Object;)V",
   "com.google.appinventor.components.runtime.util.EclairUtil.<init>()V",
   "gnu.xml.XMLPrinter.<init>(Ljava/io/Writer;Z)V",
   "gnu.xml.XMLPrinter.write(I)V",
   "gnu.xml.XMLPrinter.<init>(Ljava/io/OutputStream;)V",
   "gnu.lists.FVector.<init>()V",
   "gnu.mapping.NamedLocation.equals(Ljava/lang/Object;)Z",
   "gnu.lists.TreePosition.clone()Ljava/lang/Object;",
   "gnu.lists.AbstractFormat.parseObject(Ljava/lang/String;Ljava/text/ParsePosition;)Ljava/lang/Object;",
   "gnu.mapping.WrongType.<init>(Ljava/lang/String;ILjava/lang/ClassCastException;)V",
   "appinventor.ai_ariseo.OFWapp.Screen6.<clinit>()V",
   "gnu.text.URIPath.hashCode()I",
   "appinventor.ai_ariseo.OFWapp.Screen11.<init>()V",
   "kawa.lang.SyntaxTemplate.<init>(Ljava/lang/Object;Lkawa/lang/SyntaxForm;Lkawa/lang/Translator;)V",
   "gnu.text.RomanIntegerFormat.format(DLjava/lang/StringBuffer;Ljava/text/FieldPosition;)Ljava/lang/StringBuffer;",
   "gnu.mapping.Procedure0.<init>(Ljava/lang/String;)V",
   "gnu.mapping.OutPort.<init>(Ljava/io/Writer;ZZLgnu/text/Path;)V",
   "gnu.expr.ApplyExp.<init>(Lgnu/bytecode/Method;[Lgnu/expr/Expression;)V",
   "gnu.bytecode.Type.<init>(Ljava/lang/String;Ljava/lang/String;)V",
   "gnu.mapping.WrongArguments.<init>(Ljava/lang/String;ILjava/lang/String;)V",
   "gnu.math.Dimensions.hashCode()I",
   "gnu.text.LineBufferedReader.<init>(Ljava/io/Reader;)V",
   "gnu.kawa.lispexpr.LispReader.<init>(Lgnu/text/LineBufferedReader;)V",
   "gnu.mapping.OutPort.<init>(Ljava/io/Writer;Lgnu/text/Path;)V",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil$1.<init>(Lcom/google/appinventor/components/runtime/util/FullScreenVideoUtil;Landroid/content/Context;I)V",
   "com.google.youngandroid.runtime.<clinit>()V",
   "gnu.bytecode.ClassType.<clinit>()V",
   "gnu.math.MPN.<init>()V",
   "gnu.mapping.Procedure1.<init>(Ljava/lang/String;)V",
   "gnu.text.Path.<clinit>()V",
   "gnu.math.Numeric.equals(Ljava/lang/Object;)Z",
   "gnu.kawa.lispexpr.LispLanguage.<init>()V",
   "appinventor.ai_ariseo.OFWapp.Screen7$frame.<init>()V",
   "gnu.text.FilePath.<init>(Ljava/io/File;)V",
   "gnu.text.PrettyWriter.flush()V",
   "gnu.text.LiteralFormat.toString()Ljava/lang/String;",
   "gnu.kawa.util.AbstractHashTable.<init>()V",
   "gnu.lists.FString.<init>([CII)V",
   "gnu.expr.PrimProcedure.<init>(ILgnu/bytecode/ClassType;Ljava/lang/String;Lgnu/bytecode/Type;[Lgnu/bytecode/Type;)V",
   "gnu.math.DFloNum.<init>(Ljava/lang/String;)V",
   "gnu.mapping.ThreadLocation.<clinit>()V",
   "gnu.mapping.OutPort.<clinit>()V",
   "gnu.lists.ExtSequence.<init>()V",
   "com.google.appinventor.components.runtime.Form.onCreateOptionsMenu(Landroid/view/Menu;)Z",
   "gnu.text.Char.<init>()V",
   "gnu.mapping.UnboundLocationException.<init>()V",
   "gnu.mapping.OutPort.print(Ljava/lang/Object;)V",
   "appinventor.ai_ariseo.OFWapp.Screen8.run()V",
   "gnu.mapping.SharedLocation.<init>(Lgnu/mapping/Symbol;Ljava/lang/Object;I)V",
   "gnu.kawa.xml.NodeType.<init>(Ljava/lang/String;I)V",
   "com.google.appinventor.components.runtime.util.FullScreenVideoUtil$2.<init>(Lcom/google/appinventor/components/runtime/util/FullScreenVideoUtil;Landroid/content/Context;I)V",
   "gnu.mapping.IndirectableLocation.<init>()V",
   "gnu.mapping.PropertySet.<clinit>()V",
   "gnu.expr.ModuleManager.<init>()V",
   "gnu.expr.ModuleInfo.<clinit>()V",
   "gnu.expr.ApplyExp.<init>(Lgnu/expr/Expression;[Lgnu/expr/Expression;)V",
   "gnu.text.CompoundFormat.<init>([Ljava/text/Format;)V",
   "gnu.kawa.lispexpr.LispLanguage.<clinit>()V",
   "gnu.lists.FString.<init>(IC)V",
   "gnu.kawa.reflect.FieldLocation.<init>(Ljava/lang/Object;Ljava/lang/reflect/Field;)V",
   "gnu.kawa.util.AbstractHashTable.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
   "com.google.appinventor.components.runtime.ListPickerActivity.onListItemClick(Landroid/widget/ListView;Landroid/view/View;IJ)V",
   "gnu.lists.Pair.<init>()V",
   "kawa.lib.strings.<clinit>()V",
   "gnu.lists.FVector.<init>(I)V",
   "kawa.lib.misc.<clinit>()V",
   "kawa.lang.SyntaxPattern.<init>(Ljava/lang/Object;[Ljava/lang/Object;Lkawa/lang/Translator;)V",
   "gnu.text.Lexer.read([CII)I",
   "com.google.youngandroid.runtime$frame0.<init>()V",
   "gnu.math.DComplex.equals(Ljava/lang/Object;)Z"
};
	
	public static void main(String[] args) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		final String androidLib = "/Users/jgf/Applications/android-sdk-mac_x86/platforms/android-17/android.jar";
		final URI andLibURI = URI.create(androidLib);
		final String apkFileName = "/Users/jgf/Desktop/derr/bug-2/appinventor.ai_ariseo.OFWapp.apk";
		final URI apkFileURI = URI.create(apkFileName);
		final ClassLoader loader = RunStaticCallBug.class.getClassLoader();
		// als Android.jar benutze ich die Stub-version aus dem SDG mit API level 17 (wobei das level eigentlich egal sein sollte)
		final AnalysisScope scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(apkFileURI, "", loader, andLibURI);  // no exclusions
		final IClassHierarchy cha = ClassHierarchyFactory.make(scope);
		final RunStaticCallBug runner = new RunStaticCallBug();
		runner.buildGraphs(cha);
	}

	// build the callgraph and the SDG
	public void buildGraphs(final IClassHierarchy cha) throws FileNotFoundException, UnsoundGraphException, CancelException {
		// search for com.google.ads.ad.a([B[B)V
//		final TypeReference t = TypeReference.find(ClassLoaderReference.Application, "com.google.ads.ad");
//		final MethodReference ref = MethodReference.findOrCreate(t, "a", "([B[B)V");
//		final Set<IMethod> entryMethodSet = cha.getPossibleTargets(ref);
		final Set<IMethod> entryMethodSet = new HashSet<IMethod>();
		final Set<String> sigs = new HashSet<String>();
		sigs.addAll(Arrays.asList(ENTRIES));
		for (final IClass cls : cha) {
			if (cls.isInterface()) continue;
			for (final IMethod im : cls.getDeclaredMethods()) {
				if (im.isAbstract()) continue;
				final String s = im.getSignature();
				if (sigs.contains(s)) {
					entryMethodSet.add(im);
					sigs.remove(s);
				}
			}
		}
		
		if (sigs.isEmpty()) {
			System.out.println("All entries found.");
		} else {
			for (final String s : sigs) {
				System.out.println("Not found: " + s);
			}
		}
		
		buildAndroidCG(cha, entryMethodSet);
		sdg = buildSDG(callGraph, ptrAnalysis);
		SDGSerializer.toPDGFormat(sdg, new FileOutputStream("bug-infinite-killing.pdg"));
	}
	
	private void buildAndroidCG(final IClassHierarchy cha, Set<IMethod> entryMethodSet) {
		final ArrayList<IMethod> entryMethods = new ArrayList<IMethod>(entryMethodSet);
		// callGraph and ptrAnalysis
		final AnalysisCache cache = new AnalysisCacheImpl((IRFactory<IMethod>) new DexIRFactory());
		final AnalysisOptions options = makeAnalysisOptions(entryMethods, cha);
		final SSAPropagationCallGraphBuilder cgb =
				(ZeroXCFABuilder) WalaPointsToUtil.makeContextSensSite(options, cache, cha, cha.getScope(), Util.class.getClassLoader());

		try {
			callGraph = cgb.makeCallGraph(options);  
			ptrAnalysis = cgb.getPointerAnalysis();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}


	private List<Entrypoint> entryPointGenerator(final List<IMethod> entryMethods, final IClassHierarchy cha) {
		final List<Entrypoint> entrypoints = new LinkedList<Entrypoint>();

		for (final IMethod entryMethod: entryMethods) {
			entrypoints.add(new DexEntryPoint(entryMethod, cha));
		}

		return entrypoints;
	}

	private AnalysisOptions makeAnalysisOptions(final List<IMethod> entryMethods, final IClassHierarchy cha) {
		final List<Entrypoint> entrypoints = entryPointGenerator(entryMethods, cha);

		final AnalysisOptions analysisOptions = new AnalysisOptions(cha.getScope(), entrypoints);
		analysisOptions.setReflectionOptions(ReflectionOptions.FULL);
		analysisOptions.setHandleStaticInit(true);
		
		return analysisOptions;
	}
	   
	public static SDG buildSDG(CallGraph cg, PointerAnalysis<InstanceKey> pta) throws UnsoundGraphException, CancelException {
		long starttime = System.currentTimeMillis();
		System.out.println("=== SDG generation ===");
		SDG sdg = null;
	
		SDGBuilderConfig scfg = new SDGBuilderConfig();
		scfg.nativeSpecClassLoader = null; // callgraph has been built, already
		scfg.out = System.out;
		scfg.scope = cg.getClassHierarchy().getScope();
		scfg.cache = new AnalysisCacheImpl((IRFactory<IMethod>) new DexIRFactory());
		scfg.cha = cg.getClassHierarchy();
		//		      scfg.entry = entryMethod;
		scfg.ext = ExternalCallCheck.EMPTY;
		scfg.immutableNoOut = SDGBuilder.IMMUTABLE_NO_OUT;
		scfg.immutableStubs = SDGBuilder.IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = SDGBuilder.IGNORE_STATIC_FIELDS;
		scfg.exceptions = ExceptionAnalysis.INTERPROC;
		scfg.accessPath = false;
		scfg.prunecg = Main.DEFAULT_PRUNE_CG;//DO_NOT_PRUNE_CG;    
		scfg.pts = PointsToPrecision.OBJECT_SENSITIVE;
	   
		// StaticInitializationTreatment.ACCURATE is a buggy prototype which shouldn't be used! "Simple" models a
		// fake root class with assignments and calls to the entry points in arbitrary order! This is ok for us,
		// since the Android entry point method order is unordered!
		scfg.staticInitializers = StaticInitializationTreatment.SIMPLE; 
		scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH; // OBJ_TREE (much) too precise (and memory/performance-intensive)
		scfg.debugManyGraphsDotOutput = false;
	   
	    // compute thread information
		// since we only use the Stub/SDK version of android.jar this does only compute Thread allocation sites without generating fork edges etc.
		scfg.computeInterference = true;
	   
		sdg = SDGBuilder.build(scfg, cg, pta);
	   
		long endtime = System.currentTimeMillis();

		long timeInMillis = endtime - starttime;
		System.out.println("=== End SDG generation: " + timeInMillis + "ms ===");
		
		return sdg;
	}

}
