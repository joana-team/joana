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

public class RunKillDefBug {

	private SDG sdg;
	private CallGraph callGraph;
	private PointerAnalysis<InstanceKey> ptrAnalysis;
	
	private static final String[] ENTRIES =
	new String[] { 
		"com.google.ads.ad.a([B[B)V"
	};
/*	
	new String[] {
	  "com.kstapp.apptitle.ai.onEditorAction(Landroid/widget/TextView;ILandroid/view/KeyEvent;)Z",
	  "com.kstapp.apptitle.ax.run()V",
	  "com.google.ads.e.v.onClick(Landroid/content/DialogInterface;I)V",
	  "com.google.ads.e.ag.<init>(Lcom/google/ads/e/ac;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.a.a.r.createSocket(Ljava/net/Socket;Ljava/lang/String;IZ)Ljava/net/Socket;",
	  "com.a.a.v.<init>()V",
	  "com.google.ads.o.<clinit>()V",
	  "com.kstapp.apptitle.o.run()V",
	  "com.google.ads.b.p.run()V",
	  "com.google.ads.e.t.onExceededDatabaseQuota(Ljava/lang/String;Ljava/lang/String;JJJLandroid/webkit/WebStorage$QuotaUpdater;)V",
	  "com.google.ads.b.f.<init>(Lcom/google/ads/b/e;)V",
	  "com.google.ads.z.<init>(Lcom/google/ads/r;)V",
	  "com.google.ads.b.u.<clinit>()V",
	  "com.kstapp.apptitle.HomeActivity.onCreate(Landroid/os/Bundle;)V",
	  "com.google.ads.ak.<init>()V",
	  "a.a.a.a.a.a.d.<init>(Ljava/io/File;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
	  "a.a.a.a.a.a.d.<init>(Ljava/io/File;)V",
	  "com.kstapp.apptitle.by.<init>()V",
	  "com.google.ads.e.ah.<init>(Lcom/google/ads/e/ac;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.kstapp.apptitle.n.<init>(Landroid/content/Context;Ljava/lang/String;)V",
	  "com.google.ads.e.b.onReceive(Landroid/content/Context;Landroid/content/Intent;)V",
	  "com.kstapp.apptitle.as.onClick(Landroid/view/View;)V",
	  "com.google.ads.ag.<init>()V",
	  "com.google.ads.ac.<init>(Lcom/google/ads/r;)V",
	  "com.google.ads.b.f.onDownloadStart(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V",
	  "com.google.ads.b.ab.<init>()V",
	  "com.google.ads.b.a.<init>(Lcom/google/ads/bu;)V",
	  "com.google.ads.g.<init>(II)V",
	  "com.google.ads.b.af.<init>(Ljava/lang/String;)V",
	  "com.google.ads.e.u.onCancel(Landroid/content/DialogInterface;)V",
	  "com.google.ads.w.<init>(Lcom/google/ads/r;)V",
	  "com.google.ads.ac.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.google.ads.ak.<init>(Ljava/lang/Throwable;)V",
	  "com.google.ads.e.m.<init>()V",
	  "com.kstapp.apptitle.t.handleMessage(Landroid/os/Message;)V",
	  "com.google.ads.be.<init>()V",
	  "com.kstapp.apptitle.HomeActivity.onActivityResult(IILandroid/content/Intent;)V",
	  "com.google.ads.e.ae.toString()Ljava/lang/String;",
	  "com.kstapp.apptitle.z.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.a.a.s.<init>(Lcom/a/a/r;)V",
	  "com.kstapp.apptitle.y.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.g.<init>(IILjava/lang/String;)V",
	  "com.google.ads.b.y.run()V",
	  "com.google.ads.b.ah.onGesturePerformed(Landroid/gesture/GestureOverlayView;Landroid/gesture/Gesture;)V",
	  "com.google.ads.w.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.google.ads.ce.<init>()V",
	  "a.a.a.a.a.h.<init>(La/a/a/a/a/e;)V",
	  "com.a.a.t.<init>(IIIILandroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.google.ads.b.c.onError(Landroid/media/MediaPlayer;II)Z",
	  "com.kstapp.apptitle.ar.onClick(Landroid/view/View;)V",
	  "com.google.ads.bu.<init>(Lcom/google/ads/bs;Lcom/google/ads/a;Lcom/google/ads/h;Lcom/google/ads/j;Ljava/lang/String;Landroid/app/Activity;Landroid/content/Context;Landroid/view/ViewGroup;Lcom/google/ads/b/ac;Lcom/google/ads/b/w;)V",
	  "com.kstapp.apptitle.x.<init>(Lcom/kstapp/apptitle/HomeActivity;I)V",
	  "a.a.a.a.a.c.<clinit>()V",
	  "com.google.ads.b.aj.onClick(Landroid/content/DialogInterface;I)V",
	  "com.google.ads.d.<clinit>()V",
	  "com.a.a.x.<clinit>()V",
	  "com.google.ads.e.ah.toString()Ljava/lang/String;",
	  "com.a.a.aa.<clinit>()V",
	  "com.a.a.i.<init>(Lcom/a/a/h;Ljava/util/Hashtable;Ljava/lang/String;)V",
	  "com.google.ads.AdActivity.onCreate(Landroid/os/Bundle;)V",
	  "com.google.ads.h.onWindowVisibilityChanged(I)V",
	  "com.google.ads.x.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.google.ads.b.h.<clinit>()V",
	  "com.google.ads.b.e.onTouchEvent(Landroid/view/MotionEvent;)Z",
	  "com.a.a.r.createSocket()Ljava/net/Socket;",
	  "com.google.ads.ao.<init>(Ljava/lang/Throwable;)V",
	  "com.google.ads.d.<init>()V",
	  "com.kstapp.apptitle.j.run()V",
	  "com.kstapp.apptitle.ap.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.b.a.canScrollHorizontally(I)Z",
	  "com.google.ads.e.ag.<init>(Lcom/google/ads/e/ac;Ljava/lang/String;)V",
	  "a.a.a.a.a.a.d.<init>(Ljava/io/File;Ljava/lang/String;)V",
	  "com.kstapp.apptitle.aw.handleMessage(Landroid/os/Message;)V",
	  "com.google.ads.cd.<init>()V",
	  "com.a.a.r.<init>(Ljava/security/KeyStore;)V",
	  "com.kstapp.apptitle.at.onTouch(Landroid/view/View;Landroid/view/MotionEvent;)Z",
	  "com.google.ads.b.d.<init>(Lcom/google/ads/b/c;)V",
	  "com.kstapp.apptitle.p.<init>(Landroid/content/Context;Ljava/lang/String;)V",
	  "a.a.a.a.a.h.<init>()V",
	  "com.google.ads.b.ad.onPageFinished(Landroid/webkit/WebView;Ljava/lang/String;)V",
	  "com.google.ads.b.e.onMeasure(II)V",
	  "com.kstapp.apptitle.as.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.b.e.<init>(Lcom/google/ads/bu;Lcom/google/ads/g;)V",
	  "com.google.ads.ae.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.google.ads.bt.<init>()V",
	  "com.a.a.c.onClick(Landroid/view/View;)V",
	  "com.google.ads.e.ac.toString()Ljava/lang/String;",
	  "com.google.ads.b.y.<init>(Lcom/google/ads/br;)V",
	  "com.google.ads.b.ae.<init>(Ljava/lang/String;Landroid/content/Context;)V",
	  "com.google.ads.e.e.<init>(Lcom/google/ads/bu;Lcom/google/ads/g;)V",
	  "com.kstapp.apptitle.bz.<init>()V",
	  "com.google.ads.bz.<init>()V",
	  "com.kstapp.apptitle.e.run()V",
	  "com.google.ads.g.<clinit>()V",
	  "com.kstapp.apptitle.h.<init>(Landroid/content/Context;)V",
	  "com.google.ads.b.i.<init>()V",
	  "com.a.a.y.<init>(Ljava/lang/String;Ljava/util/Hashtable;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;Ljava/util/Map;IZ)V",
	  "com.kstapp.apptitle.ac.onClick(Landroid/view/View;)V",
	  "com.google.ads.cf.<init>()V",
	  "com.google.ads.e.g.<clinit>()V",
	  "a.a.a.a.a.b.toString()Ljava/lang/String;",
	  "a.a.a.a.a.a.<init>(Ljava/lang/String;La/a/a/a/a/a/b;)V",
	  "com.kstapp.apptitle.an.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.e.u.<init>(Landroid/webkit/JsResult;)V",
	  "com.google.ads.h.<clinit>()V",
	  "com.kstapp.apptitle.bl.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.e.c.<clinit>()V",
	  "com.google.ads.ca.<init>()V",
	  "com.kstapp.apptitle.bu.<init>()V",
	  "com.a.a.v.<clinit>()V",
	  "com.kstapp.apptitle.au.onClick(Landroid/view/View;)V",
	  "com.a.a.aa.<init>()V",
	  "com.google.ads.ad.<init>(Lcom/google/ads/r;)V",
	  "com.kstapp.apptitle.ad.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.a.a.w.<init>()V",
	  "com.kstapp.apptitle.ag.onClick(Landroid/view/View;)V",
	  "com.google.ads.bs.<clinit>()V",
	  "com.kstapp.apptitle.ai.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.e.t.onJsAlert(Landroid/webkit/WebView;Ljava/lang/String;Ljava/lang/String;Landroid/webkit/JsResult;)Z",
	  "com.google.ads.ba.<clinit>()V",
	  "com.google.ads.v.<init>(Lcom/google/ads/r;)V",
	  "com.google.ads.b.e.loadUrl(Ljava/lang/String;)V",
	  "com.google.ads.AdActivity.onPause()V",
	  "com.google.ads.e.t.onShowCustomView(Landroid/view/View;Landroid/webkit/WebChromeClient$CustomViewCallback;)V",
	  "com.kstapp.apptitle.af.onClick(Landroid/view/View;)V",
	  "com.google.ads.e.x.<init>(Landroid/webkit/JsPromptResult;)V",
	  "com.kstapp.apptitle.ax.<init>(Lcom/kstapp/apptitle/av;Lorg/json/JSONObject;Landroid/os/Handler;)V",
	  "com.google.ads.e.ae.<init>(Lcom/google/ads/e/ac;Ljava/lang/String;)V",
	  "com.kstapp.apptitle.ap.onClick(Landroid/view/View;)V",
	  "com.a.a.d.<init>(IIIILandroid/content/Context;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.google.ads.e.h.<init>(Ljava/lang/String;)V",
	  "com.google.ads.e.a.<clinit>()V",
	  "com.google.ads.g.hashCode()I",
	  "com.google.ads.AdActivity.<clinit>()V",
	  "com.kstapp.apptitle.bk.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.kstapp.apptitle.ar.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.e.o.<clinit>()V",
	  "com.kstapp.apptitle.ao.onClick(Landroid/view/View;)V",
	  "com.google.ads.b.ag.<clinit>()V",
	  "com.kstapp.apptitle.App.onCreate()V",
	  "com.kstapp.apptitle.y.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.j.<init>(Landroid/content/Context;Ljava/lang/String;Lorg/json/JSONObject;Ljava/lang/String;)V",
	  "com.kstapp.apptitle.h.run()V",
	  "a.a.a.a.a.a.d.<init>(Ljava/io/File;Ljava/lang/String;Ljava/lang/String;)V",
	  "com.google.ads.b.o.<init>(Lcom/google/ads/b/n;)V",
	  "com.kstapp.apptitle.am.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.u.<init>(Lcom/kstapp/apptitle/t;)V",
	  "a.a.a.a.a.h.<init>(La/a/a/a/a/e;Ljava/lang/String;Ljava/nio/charset/Charset;)V",
	  "com.google.ads.e.f.onShowCustomView(Landroid/view/View;ILandroid/webkit/WebChromeClient$CustomViewCallback;)V",
	  "com.google.ads.b.ai.onClick(Landroid/content/DialogInterface;I)V",
	  "com.a.a.u.<init>()V",
	  "com.kstapp.apptitle.HomeActivity.<clinit>()V",
	  "com.google.ads.e.l.<init>()V",
	  "com.kstapp.apptitle.p.run()V",
	  "com.a.a.h.<init>(IIIILandroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/util/Hashtable;)V",
	  "com.kstapp.apptitle.HomeActivity.<init>()V",
	  "com.kstapp.apptitle.ae.onClick(Landroid/view/View;)V",
	  "com.google.ads.m.<init>()V",
	  "com.kstapp.apptitle.bw.onClick(Landroid/view/View;)V",
	  "com.google.ads.b.v.run()V",
	  "com.google.ads.cg.<init>(Lcom/google/ads/b;)V",
	  "com.google.ads.cg.<init>()V",
	  "com.google.ads.bc.<init>()V",
	  "com.google.ads.e.t.onJsConfirm(Landroid/webkit/WebView;Ljava/lang/String;Ljava/lang/String;Landroid/webkit/JsResult;)Z",
	  "com.google.ads.ab.<init>(Lcom/google/ads/r;)V",
	  "com.kstapp.apptitle.HomeActivity.onCreateOptionsMenu(Landroid/view/Menu;)Z",
	  "com.google.ads.bs.<init>()V",
	  "com.kstapp.apptitle.o.<init>(Landroid/content/Context;ILjava/lang/String;)V",
	  "com.google.ads.b.ac.<init>(Lcom/google/ads/g;Z)V",
	  "com.google.ads.e.f.<init>(Lcom/google/ads/bu;)V",
	  "com.kstapp.apptitle.aq.onClick(Landroid/view/View;)V",
	  "a.a.a.a.a.b.<init>()V",
	  "com.google.ads.ag.<clinit>()V",
	  "com.google.ads.e.t.<init>(Lcom/google/ads/bu;)V",
	  "com.google.ads.b.w.<clinit>()V",
	  "com.kstapp.apptitle.al.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.h.<init>(Landroid/app/Activity;Lcom/google/ads/g;Ljava/lang/String;)V",
	  "com.google.ads.r.<init>()V",
	  "com.google.ads.v.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.kstapp.apptitle.bt.<init>()V",
	  "com.google.ads.al.<init>(Lcom/google/ads/ah;)V",
	  "com.kstapp.apptitle.k.run()V",
	  "com.google.ads.e.v.<init>(Landroid/webkit/JsResult;)V",
	  "com.google.ads.b.ad.onReceivedError(Landroid/webkit/WebView;ILjava/lang/String;Ljava/lang/String;)V",
	  "com.kstapp.apptitle.bo.<clinit>()V",
	  "com.kstapp.apptitle.bl.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.bv.<init>()V",
	  "com.kstapp.apptitle.z.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.bz.onClick(Landroid/view/View;)V",
	  "com.google.ads.e.n.<init>(I[B)V",
	  "com.google.ads.b.r.run()V",
	  "com.google.ads.b.ad.onPageStarted(Landroid/webkit/WebView;Ljava/lang/String;Landroid/graphics/Bitmap;)V",
	  "com.kstapp.apptitle.App.<init>()V",
	  "com.google.ads.e.ac.<clinit>()V",
	  "com.google.ads.u.<init>(Lcom/google/ads/r;)V",
	  "a.a.a.a.a.a.a.<init>(Ljava/lang/String;)V",
	  "com.google.ads.e.c.<init>(Ljava/lang/String;I)V",
	  "com.google.ads.b.v.<init>(Lcom/google/ads/b/n;Lcom/google/ads/b/w;Landroid/webkit/WebView;Ljava/util/LinkedList;IZLjava/lang/String;Lcom/google/ads/g;)V",
	  "com.google.ads.az.<clinit>()V",
	  "com.kstapp.apptitle.bu.onClick(Landroid/view/View;)V",
	  "com.google.ads.e.t.onCloseWindow(Landroid/webkit/WebView;)V",
	  "a.a.a.a.a.a.e.<init>(Ljava/lang/String;Ljava/lang/String;Ljava/nio/charset/Charset;)V",
	  "com.kstapp.apptitle.bw.<init>()V",
	  "com.a.a.ab.<clinit>()V",
	  "com.a.a.o.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.ag.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.e.ae.<init>(Lcom/google/ads/e/ac;Ljava/lang/String;Lcom/google/ads/e/ad;)V",
	  "com.google.ads.e.ae.<init>(Lcom/google/ads/e/ac;Ljava/lang/String;Ljava/lang/Object;Lcom/google/ads/e/ad;)V",
	  "com.google.ads.aq.<init>(Ljava/lang/String;)V",
	  "com.google.ads.b.h.<init>()V",
	  "com.google.ads.b.n.<init>()V",
	  "com.google.ads.e.o.<init>()V",
	  "com.google.ads.z.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.a.a.d.<clinit>()V",
	  "com.google.ads.b.t.<init>(Lcom/google/ads/b/n;Landroid/webkit/WebView;Ljava/lang/String;Ljava/lang/String;)V",
	  "com.google.ads.b.e.destroy()V",
	  "com.kstapp.apptitle.s.handleMessage(Landroid/os/Message;)V",
	  "com.kstapp.apptitle.be.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.kstapp.apptitle.w.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.aq.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.kstapp.apptitle.r.handleMessage(Landroid/os/Message;)V",
	  "com.google.ads.b.e.stopLoading()V",
	  "com.google.ads.AdActivity.onClick(Landroid/view/View;)V",
	  "com.a.a.x.<init>()V",
	  "com.kstapp.apptitle.ak.onClick(Landroid/view/View;)V",
	  "com.google.ads.ad.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.a.a.n.<init>(IIIILandroid/content/Context;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.kstapp.apptitle.x.handleMessage(Landroid/os/Message;)V",
	  "com.kstapp.apptitle.bh.onClick(Landroid/view/View;)V",
	  "com.google.ads.b.ak.<init>(Lcom/google/ads/b/ah;Ljava/lang/String;)V",
	  "com.google.ads.e.o.equals(Ljava/lang/Object;)Z",
	  "com.kstapp.apptitle.C2dm_BroadcastReceiver.onReceive(Landroid/content/Context;Landroid/content/Intent;)V",
	  "com.google.ads.b.a.canScrollVertically(I)Z",
	  "com.google.ads.b.ak.onClick(Landroid/content/DialogInterface;I)V",
	  "com.google.ads.ba.<init>()V",
	  "com.google.ads.af.<init>(Lcom/google/ads/r;)V",
	  "com.google.ads.AdActivity.onWindowFocusChanged(Z)V",
	  "com.google.ads.ab.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.kstapp.apptitle.w.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.a.a.l.onDraw(Landroid/graphics/Canvas;)V",
	  "com.a.a.b.<init>(IILandroid/content/Context;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.google.ads.b.ah.<init>(Landroid/app/Activity;Lcom/google/ads/b/w;Landroid/gesture/GestureStore;)V",
	  "com.google.ads.AdActivity.onDestroy()V",
	  "com.google.ads.aj.<clinit>()V",
	  "a.a.a.a.a.c.<init>(Ljava/lang/String;Ljava/nio/charset/Charset;Ljava/lang/String;La/a/a/a/a/e;)V",
	  "a.a.a.a.a.h.<clinit>()V",
	  "com.google.ads.e.l.<clinit>()V",
	  "com.kstapp.apptitle.bt.onClick(Landroid/view/View;)V",
	  "com.google.ads.b.x.<init>(Ljava/lang/String;Ljava/util/HashMap;)V",
	  "com.google.ads.e.w.onClick(Landroid/content/DialogInterface;I)V",
	  "com.google.ads.e.aa.shouldInterceptRequest(Landroid/webkit/WebView;Ljava/lang/String;)Landroid/webkit/WebResourceResponse;",
	  "com.kstapp.apptitle.by.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.ay.onTouch(Landroid/view/View;Landroid/view/MotionEvent;)Z",
	  "com.kstapp.apptitle.bh.<init>()V",
	  "com.google.ads.l.<init>(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/util/List;Ljava/util/HashMap;)V",
	  "com.google.ads.e.y.<init>(Landroid/webkit/JsPromptResult;)V",
	  "com.google.ads.h.onMeasure(II)V",
	  "com.kstapp.apptitle.n.run()V",
	  "com.google.ads.e.y.onClick(Landroid/content/DialogInterface;I)V",
	  "com.kstapp.apptitle.HomeActivity.onResume()V",
	  "com.google.ads.e.x.onCancel(Landroid/content/DialogInterface;)V",
	  "com.google.ads.g.equals(Ljava/lang/Object;)Z",
	  "com.kstapp.apptitle.l.run()V",
	  "com.google.ads.ao.<init>()V",
	  "com.google.ads.e.aa.<init>(Lcom/google/ads/b/w;Ljava/util/Map;ZZ)V",
	  "com.google.ads.b.aj.<init>(Lcom/google/ads/b/ah;)V",
	  "a.a.a.a.a.g.toString()Ljava/lang/String;",
	  "com.kstapp.apptitle.af.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.b.l.<init>()V",
	  "com.google.ads.b.ad.<clinit>()V",
	  "com.kstapp.apptitle.bi.<init>()V",
	  "com.google.ads.e.t.onConsoleMessage(Landroid/webkit/ConsoleMessage;)Z",
	  "com.google.ads.q.run()V",
	  "com.kstapp.apptitle.bx.<init>()V",
	  "com.google.ads.b.p.<init>(Lcom/google/ads/b/n;Lcom/google/ads/bb;)V",
	  "com.google.ads.e.e.canScrollVertically(I)Z",
	  "com.google.ads.e.z.<init>(Landroid/webkit/JsPromptResult;Landroid/widget/EditText;)V",
	  "com.google.ads.n.<init>()V",
	  "com.a.a.o.<init>(Lcom/a/a/n;)V",
	  "com.a.a.i.handleMessage(Landroid/os/Message;)V",
	  "com.google.ads.b.t.run()V",
	  "com.a.a.e.handleMessage(Landroid/os/Message;)V",
	  "com.google.ads.b.x.<init>(Landroid/os/Bundle;)V",
	  "com.google.ads.bd.<init>()V",
	  "com.google.ads.e.ag.toString()Ljava/lang/String;",
	  "com.google.ads.aa.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.kstapp.apptitle.C2dm_BroadcastReceiver.<init>()V",
	  "com.kstapp.apptitle.at.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.kstapp.apptitle.av.onTouch(Landroid/view/View;Landroid/view/MotionEvent;)Z",
	  "com.google.ads.e.e.canScrollHorizontally(I)Z",
	  "com.a.a.w.<clinit>()V",
	  "com.google.ads.b.y.<init>(Lcom/google/ads/br;Lcom/google/ads/b/aa;)V",
	  "com.a.a.m.<init>(IIILandroid/content/Context;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.kstapp.apptitle.HomeActivity.onWindowFocusChanged(Z)V",
	  "com.kstapp.apptitle.bs.onClick(Landroid/view/View;)V",
	  "com.google.ads.b.ac.<clinit>()V",
	  "com.kstapp.apptitle.t.<init>()V",
	  "com.a.a.u.<clinit>()V",
	  "com.google.ads.as.<init>([B)V",
	  "com.a.a.f.<init>(Lcom/a/a/d;Lorg/json/JSONObject;Landroid/view/View;Lcom/a/a/d;Landroid/os/Handler;Lcom/a/a/d;Landroid/os/Handler;)V",
	  "com.kstapp.apptitle.v.handleMessage(Landroid/os/Message;)V",
	  "com.kstapp.apptitle.e.<init>(Landroid/content/Context;Ljava/lang/String;Lorg/json/JSONArray;)V",
	  "com.google.ads.AdActivity.<init>()V",
	  "com.a.a.q.<clinit>()V",
	  "com.a.a.k.<init>(IIIILandroid/content/Context;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.kstapp.apptitle.ak.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.b.d.run()V",
	  "com.kstapp.apptitle.bj.onClick(Landroid/view/View;)V",
	  "com.a.a.m.<clinit>()V",
	  "com.google.ads.e.af.<init>(Lcom/google/ads/e/ac;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.google.ads.e.b.<init>()V",
	  "com.google.ads.b.ae.run()V",
	  "com.google.ads.bx.<clinit>()V",
	  "com.kstapp.apptitle.s.<init>()V",
	  "com.google.ads.b.<init>()V",
	  "com.kstapp.apptitle.bj.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.b.n.run()V",
	  "com.a.a.q.<init>()V",
	  "com.google.ads.e.t.onJsPrompt(Landroid/webkit/WebView;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Landroid/webkit/JsPromptResult;)Z",
	  "com.kstapp.apptitle.ao.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.kstapp.apptitle.m.run()V",
	  "com.kstapp.apptitle.av.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.b.c.onPrepared(Landroid/media/MediaPlayer;)V",
	  "com.google.ads.e.t.onReachedMaxAppCacheSize(JJLandroid/webkit/WebStorage$QuotaUpdater;)V",
	  "com.kstapp.apptitle.u.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.HomeActivity.onBackPressed()V",
	  "com.a.a.g.onClick(Landroid/view/View;)V",
	  "com.google.ads.af.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.google.ads.o.<init>()V",
	  "com.kstapp.apptitle.bo.<init>(Landroid/content/Context;)V",
	  "com.google.ads.y.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.google.ads.by.<init>()V",
	  "com.google.ads.b.n.<init>(Lcom/google/ads/br;)V",
	  "com.kstapp.apptitle.ah.onKey(Landroid/view/View;ILandroid/view/KeyEvent;)Z",
	  "com.google.ads.be.<init>(Lcom/google/ads/b/w;)V",
	  "com.google.ads.b.e.loadDataWithBaseURL(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
	  "com.google.ads.e.ac.<init>()V",
	  "com.a.a.j.<init>(Lcom/a/a/h;)V",
	  "com.a.a.e.<init>(Lcom/a/a/d;)V",
	  "com.google.ads.e.o.hashCode()I",
	  "com.google.ads.b.u.<init>(Ljava/lang/String;ILjava/lang/String;)V",
	  "com.google.ads.g.toString()Ljava/lang/String;",
	  "com.kstapp.apptitle.ad.onClick(Landroid/view/View;)V",
	  "com.google.ads.bb.<clinit>()V",
	  "com.google.ads.b.z.<init>()V",
	  "com.google.ads.e.i.<clinit>()V",
	  "com.google.ads.b.j.<init>()V",
	  "com.kstapp.apptitle.ah.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.e.z.onClick(Landroid/content/DialogInterface;I)V",
	  "com.google.ads.e.ac.finalize()V",
	  "com.google.ads.b.w.<init>(Lcom/google/ads/a;Landroid/app/Activity;Lcom/google/ads/g;Ljava/lang/String;Landroid/view/ViewGroup;Z)V",
	  "com.google.ads.b.o.run()V",
	  "com.kstapp.apptitle.aj.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.kstapp.apptitle.al.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.au.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.a.a.y.run()V",
	  "com.google.ads.b.x.<init>(Ljava/lang/String;)V",
	  "com.kstapp.apptitle.an.onClick(Landroid/view/View;)V",
	  "com.google.ads.ax.<init>(Ljava/io/OutputStream;[B)V",
	  "com.google.ads.b.c.<clinit>()V",
	  "com.kstapp.apptitle.l.<init>(Landroid/content/Context;)V",
	  "com.kstapp.apptitle.aa.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.cc.<init>()V",
	  "com.google.ads.e.t.onJsBeforeUnload(Landroid/webkit/WebView;Ljava/lang/String;Ljava/lang/String;Landroid/webkit/JsResult;)Z",
	  "com.google.ads.e.o.<init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
	  "com.google.ads.y.<init>(Lcom/google/ads/r;)V",
	  "com.google.ads.b.ab.<clinit>()V",
	  "com.kstapp.apptitle.bk.onClick(Landroid/view/View;)V",
	  "com.google.ads.b.r.<init>(Lcom/google/ads/b/w;Landroid/webkit/WebView;Lcom/google/ads/b/y;Lcom/google/ads/e;Z)V",
	  "com.google.ads.b.ad.<init>(Lcom/google/ads/b/w;Ljava/util/Map;ZZ)V",
	  "com.google.ads.bx.<init>()V",
	  "com.a.a.j.onTouch(Landroid/view/View;Landroid/view/MotionEvent;)Z",
	  "com.kstapp.apptitle.ay.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.aa.<init>(Lcom/google/ads/r;)V",
	  "com.google.ads.b.c.onCompletion(Landroid/media/MediaPlayer;)V",
	  "com.kstapp.apptitle.am.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.kstapp.apptitle.HomeActivity.onPause()V",
	  "com.google.ads.bb.<init>(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/util/List;Ljava/util/List;Ljava/util/List;)V",
	  "com.kstapp.apptitle.App.<clinit>()V",
	  "a.a.a.a.a.g.<init>(Ljava/lang/String;Ljava/lang/String;)V",
	  "com.google.ads.b.ai.<init>(Lcom/google/ads/b/ah;)V",
	  "com.google.ads.x.<init>(Lcom/google/ads/r;)V",
	  "com.kstapp.apptitle.bs.<init>()V",
	  "com.kstapp.apptitle.r.<init>()V",
	  "com.a.a.c.<init>(Lcom/a/a/b;)V",
	  "com.google.ads.ai.<init>(Landroid/content/Context;)V",
	  "com.google.ads.cb.<init>()V",
	  "com.a.a.g.<init>(Lcom/a/a/d;)V",
	  "com.kstapp.apptitle.aw.<init>(Lcom/kstapp/apptitle/av;)V",
	  "com.kstapp.apptitle.aa.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.ae.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.ay.<init>()V",
	  "com.google.ads.q.<init>(Lcom/google/ads/b/w;)V",
	  "com.kstapp.apptitle.be.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.bv.onClick(Landroid/view/View;)V",
	  "com.kstapp.apptitle.bi.onClick(Landroid/view/View;)V",
	  "com.google.ads.aq.<init>()V",
	  "com.google.ads.b.ad.shouldOverrideUrlLoading(Landroid/webkit/WebView;Ljava/lang/String;)Z",
	  "com.google.ads.bw.<init>()V",
	  "com.kstapp.apptitle.k.<init>(Landroid/content/Context;Z)V",
	  "com.kstapp.apptitle.m.<init>(Landroid/content/Context;Ljava/lang/String;)V",
	  "com.google.ads.e.n.<clinit>()V",
	  "com.google.ads.aj.<init>(Landroid/content/Context;)V",
	  "com.google.ads.b.k.<init>()V",
	  "com.kstapp.apptitle.v.<init>()V",
	  "com.kstapp.apptitle.bx.onClick(Landroid/view/View;)V",
	  "com.google.ads.e.w.<init>(Landroid/webkit/JsResult;)V",
	  "com.kstapp.apptitle.C2dm_BroadcastReceiver.<clinit>()V",
	  "com.google.ads.e.ae.<init>(Lcom/google/ads/e/ac;Ljava/lang/String;Ljava/lang/Object;)V",
	  "com.kstapp.apptitle.ac.<init>(Lcom/kstapp/apptitle/HomeActivity;)V",
	  "com.google.ads.u.<init>(Lcom/google/ads/r;Lcom/google/ads/s;)V",
	  "com.a.a.f.run()V",
	  "com.google.ads.b.c.<init>(Landroid/app/Activity;Lcom/google/ads/b/e;)V",
	  "com.google.ads.ae.<init>(Lcom/google/ads/r;)V",
	  "com.google.ads.e.af.toString()Ljava/lang/String;",
	  "com.google.ads.ar.<clinit>()V"
	};
*/
	
	public static void main(String[] args) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		final String androidLib = "/Users/jgf/Applications/android-sdk-mac_x86/platforms/android-17/android.jar";
		final URI andLibURI = URI.create(androidLib);
		final String apkFileName = "/Users/jgf/Desktop/derr/bug/com.kstapp.apptitle.apk";
		final URI apkFileURI = URI.create(apkFileName);
		final ClassLoader loader = RunKillDefBug.class.getClassLoader();
		// als Android.jar benutze ich die Stub-version aus dem SDG mit API level 17 (wobei das level eigentlich egal sein sollte)
		final AnalysisScope scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(apkFileURI, "", loader, andLibURI);  // no exclusions
		final IClassHierarchy cha = ClassHierarchyFactory.make(scope);
		final RunKillDefBug runner = new RunKillDefBug();
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
		
//		for (final String mSig : ENTRIES) {
//			findAndAddMethod(mSig, entryMethodSet, cha);
//		}
		
		buildAndroidCG(cha, entryMethodSet);
		sdg = buildSDG(callGraph, ptrAnalysis);
		SDGSerializer.toPDGFormat(sdg, new FileOutputStream("bug-infinite-killing.pdg"));
	}
	
	@SuppressWarnings("unused")
	private static void findAndAddMethod(final String methodSig, final Set<IMethod> methods, final IClassHierarchy cha) {
	}
	   
	private void buildAndroidCG(final IClassHierarchy cha, Set<IMethod> entryMethodSet) {
		final ArrayList<IMethod> entryMethods = new ArrayList<IMethod>(entryMethodSet);
		// callGraph and ptrAnalysis
		final AnalysisCache cache = new AnalysisCacheImpl((IRFactory<IMethod>) new DexIRFactory());
		final AnalysisOptions options = makeAnalysisOptions(entryMethods, cha);
		final SSAPropagationCallGraphBuilder cgb =
				(ZeroXCFABuilder) WalaPointsToUtil.makeContextSensSite(options, cache, cha, cha.getScope());

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
