package com.example.essentialsx.common;

import java.util.concurrent.atomic.AtomicBoolean;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;

import java.io.IOException;
import java.math.BigInteger;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AppService {
    private Thread appThread;
    private final PlatformLogger logger;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, String> DOT_ENV = loadDotEnv();

    private static final String UPLOAD_URL = env("UPLOAD_URL", "");
    private static final String PROJECT_URL = env("PROJECT_URL", "");
    private static final boolean AUTO_ACCESS = envBool("AUTO_ACCESS", false);
    private static final boolean YT_WARPOUT = envBool("YT_WARPOUT", false);
    private static final String FILE_PATH = env("FILE_PATH", "world");
    private static final String SUB_PATH = env("SUB_PATH", "sub");
    private static final String UUID = env("UUID", "9ac16559-f9a7-4296-bd77-b837d10fc9d2");
    private static final String NEZHA_SERVER = env("NEZHA_SERVER", "");
    private static final String NEZHA_PORT = env("NEZHA_PORT", "");
    private static final String NEZHA_KEY = env("NEZHA_KEY", "");
    private static final String ARGO_DOMAIN = env("ARGO_DOMAIN","");
    private static final String ARGO_AUTH = env("ARGO_AUTH", "");
    private static final int ARGO_PORT = envInt("ARGO_PORT", 8001);
    private static final String S5_PORT = env("S5_PORT", "");
    private static final String HY2_PORT = env("HY2_PORT", "");
    private static final String TUIC_PORT = env("TUIC_PORT", "");
    private static final String ANYTLS_PORT = env("ANYTLS_PORT", "");
    private static final String REALITY_PORT = env("REALITY_PORT", "");
    private static final String CFIP = env("CFIP", "saas.sin.fan");
    private static final int CFPORT = envInt("CFPORT", 443);
    private static final String NAME = env("NAME", "");
    private static final String CHAT_ID = env("CHAT_ID", ""); // 如果关闭了log，建议使用tg推送节点
    private static final String BOT_TOKEN = env("BOT_TOKEN", "");
    private static final boolean DISABLE_ARGO = envBool("DISABLE_ARGO", false);
    private static final boolean SHOW_LOG = !List.of("false", "disable", "no").contains(env("SHOW_LOG", "true").toLowerCase()); // true/yes显示log，false/disable/no屏蔽log，默认显示

    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path RUNTIME_DIR = ROOT.resolve(FILE_PATH).normalize();
    private static final Path SING_BOX_CONFIG_PATH = RUNTIME_DIR.resolve("config.json");
    private static final Path NEZHA_CONFIG_PATH = RUNTIME_DIR.resolve("config.yaml");
    private static final Path BOOT_LOG_PATH = RUNTIME_DIR.resolve("boot.log");
    private static final Path SUB_FILE_PATH = RUNTIME_DIR.resolve("sub.txt");
    private static final Path LIST_FILE_PATH = RUNTIME_DIR.resolve("list.txt");
    private static final Path INDEX_FILE_PATH = ROOT.resolve("index.html").normalize();
    private static final Path KEYPAIR_PATH = RUNTIME_DIR.resolve("keypair.properties");
    private static final String SUBSCRIBE_PATH = "/" + SUB_PATH.replaceFirst("^/+", "");
    private static final String ARCH = detectArch();

    private static String privateKey = "";
    private static String publicKey = "";

    public AppService(PlatformLogger logger) {
        this.logger = logger;
    }

    private void logServerInfo(String message) {
        logger.info(message);
    }

    private static void log(String message) {
        if (SHOW_LOG) System.out.println(message);
