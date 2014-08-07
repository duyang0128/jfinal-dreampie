package cn.dreampie.common.plugin.lesscss;

import cn.dreampie.common.plugin.lesscss.compiler.LessCssCompiler;
import cn.dreampie.common.plugin.lesscss.compiler.LessExecuteListener;
import cn.dreampie.common.plugin.lesscss.compiler.LessExecuteThread;
import com.jfinal.kit.PathKit;
import com.jfinal.plugin.IPlugin;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by wangrenhui on 2014/7/11.
 */
public class LessCssPlugin implements IPlugin {

  private Logger logger = LoggerFactory.getLogger(getClass());
  //restart thread  timeout
  private int restartInterval = 10000;
  private LessCssCompiler lessCssCompiler;

  public LessCssPlugin() {
    lessCssCompiler = new LessCssCompiler();
    lessCssCompiler.setBuildContext(ThreadBuildContext.getContext());
    lessCssCompiler.setSourceDirectory(new File(PathKit.getWebRootPath() + "/style/"));
    lessCssCompiler.setOutputDirectory(new File(PathKit.getWebRootPath() + "/style/"));
//        lessCssCompiler.setForce(true);
//        lessCssCompiler.setCompress(true);
    lessCssCompiler.setWatch(true);
  }

  public LessCssPlugin(LessCssCompiler lessCssCompiler) {
    this.lessCssCompiler = lessCssCompiler;
  }

  public LessCssPlugin(int restartInterval, LessCssCompiler lessCssCompiler) {
    this.restartInterval = restartInterval;
    this.lessCssCompiler = lessCssCompiler;
  }

  @Override
  public boolean start() {


    LessExecuteThread run = new LessExecuteThread(lessCssCompiler,restartInterval);
    LessExecuteListener listen = new LessExecuteListener(run);
    run.addObserver(listen);
    new Thread(run).start();
    return true;
  }

  @Override
  public boolean stop() {
    return false;
  }

  public static void main(String[] args) throws LessException, IOException {
    LessCompiler lessCompiler = new LessCompiler(Arrays.asList("--relative-urls", "--strict-math=on"));
    String css = lessCompiler.compile("@color: #4D926F; #header { color: @color; }");
//        System.out.println(css);


    lessCompiler = new LessCompiler(Arrays.asList("--relative-urls", "--strict-math=on"));
    css = lessCompiler.compile(new File(PathKit.getWebRootPath() + "/src/main/webapp/css/app/_layout.less"));
    System.out.println(css);

    LessCssCompiler lessCssCompiler = new LessCssCompiler();
    lessCssCompiler.setBuildContext(ThreadBuildContext.getContext());
    lessCssCompiler.setSourceDirectory(new File(PathKit.getWebRootPath() + "/css/"));
    lessCssCompiler.setOutputDirectory(new File(PathKit.getRootClassPath() + "/css/"));
    lessCssCompiler.setForce(true);
    lessCssCompiler.setCompress(true);
    lessCssCompiler.setWatch(true);
    lessCssCompiler.execute();
  }
}
