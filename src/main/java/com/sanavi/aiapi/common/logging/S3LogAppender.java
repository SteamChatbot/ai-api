package com.sanavi.aiapi.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// 책임: 로그 이벤트를 버퍼에 모아 S3에 업로드하는 Logback Appender
//       AppenderBase를 상속 — doAppend()가 내부적으로 synchronized 처리됨
//       S3는 append 연산을 지원하지 않으므로 버퍼가 찼을 때 새 파일로 업로드
// S3 저장 경로: {folderPath}/{yyyy-MM-dd}/{HH-mm-ss}-{uuid8}.log
// 예시: log/sanavi-ai-api/2026-06-25/14-23-01-a3f9c1b2.log
// 주의: 이 클래스 안에서 log.xxx() 사용 금지 — 무한 순환 호출 발생
//       S3 업로드 실패 등 내부 오류는 System.err로만 출력
public class S3LogAppender extends AppenderBase<ILoggingEvent> {

    // logback-spring.xml에서 <bucketName>, <region>, <folderPath>, <flushThreshold> 태그로 주입
    private String bucketName;
    private String region;
    private String folderPath;
    // 이 줄 수만큼 쌓이면 S3에 플러시 — 너무 작으면 API 호출 과다, 너무 크면 로그 유실 위험
    private int flushThreshold = 50;

    private S3Client s3Client;
    // AppenderBase.doAppend()가 synchronized이므로 buffer는 단일 스레드에서만 접근됨
    private final List<String> buffer = new ArrayList<>();

    private static final DateTimeFormatter LOG_TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));
    private static final DateTimeFormatter FILE_TS_FMT =
            DateTimeFormatter.ofPattern("HH-mm-ss");

    // Input:  없음
    // Output: 없음
    // 책임:   S3Client 초기화 + JVM 종료 시 남은 버퍼 플러시하는 셧다운 훅 등록
    @Override
    public void start() {
        // Spring Bean이 아닌 직접 생성 — Logback은 Spring 컨텍스트보다 먼저 초기화되므로 DI 불가
        s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        // JVM 종료 시 버퍼에 남은 로그가 유실되지 않도록 플러시
        Runtime.getRuntime().addShutdownHook(new Thread(this::flush, "s3-log-shutdown"));

        super.start();
    }

    // Input:  ILoggingEvent (로그 이벤트 — 레벨, 메시지, MDC, 타임스탬프 포함)
    // Output: 없음
    // 책임:   이벤트를 포맷해 버퍼에 추가, flushThreshold 초과 시 S3 플러시
    @Override
    protected void append(ILoggingEvent event) {
        buffer.add(format(event));
        if (buffer.size() >= flushThreshold) {
            flush();
        }
    }

    // Input:  없음
    // Output: 없음
    // 책임:   Appender 종료 시 남은 버퍼 플러시 후 S3Client 반환
    @Override
    public void stop() {
        flush();
        if (s3Client != null) {
            s3Client.close();
        }
        super.stop();
    }

    // Input:  없음
    // Output: 없음
    // 책임:   버퍼 내용을 하나의 문자열로 합쳐 S3에 업로드 후 버퍼 비움
    //         S3는 append 미지원 → 플러시 시점마다 타임스탬프+UUID로 새 파일 생성
    private synchronized void flush() {
        if (buffer.isEmpty()) return;

        String content = String.join("", buffer);
        buffer.clear();

        // 키 예: log/sanavi-ai-api/2026-06-25/14-23-01-a3f9c1b2.log
        String datePath = LocalDate.now().toString();
        String timePart = LocalDateTime.now().format(FILE_TS_FMT);
        String uuidPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String key = folderPath + "/" + datePath + "/" + timePart + "-" + uuidPart + ".log";

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .contentType("text/plain; charset=utf-8")
                            .build(),
                    RequestBody.fromString(content, StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            // log.error 사용 금지 — 이 Appender 자신이 호출되어 무한 순환 발생
            System.err.println("[S3LogAppender] S3 업로드 실패 key=" + key + " : " + e.getMessage());
        }
    }

    // Input:  ILoggingEvent
    // Output: 한 줄 로그 문자열 — "yyyy-MM-dd HH:mm:ss [traceId] LEVEL LoggerName — message\n"
    private String format(ILoggingEvent event) {
        String traceId = event.getMDCPropertyMap().getOrDefault("traceId", "-");
        String timestamp = LOG_TS_FMT.format(Instant.ofEpochMilli(event.getTimeStamp()));
        return String.format("%s [%s] %-5s %s — %s%n",
                timestamp, traceId, event.getLevel(), event.getLoggerName(), event.getFormattedMessage());
    }

    // Logback XML에서 필드 주입 시 setter 필요
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
    public void setRegion(String region) { this.region = region; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }
    public void setFlushThreshold(int flushThreshold) { this.flushThreshold = flushThreshold; }
}
