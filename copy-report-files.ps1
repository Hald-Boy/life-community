# ============================================================
#  作业脚本：把单体 report 域代码复制到 report-service
#  使用方法：右键"使用 PowerShell 运行"，或：
#    powershell -ExecutionPolicy Bypass -File copy-report-files.ps1
#  执行完后会自动把包名 com.guat.mynewsapp 换成 com.guat.cloud.report
# ============================================================
$ErrorActionPreference = "Stop"

$src = "D:\Mysoft\IT\javaCode\News_CourseDesign\src\main\java\com\guat\mynewsapp"
$dst = "D:\Mysoft\IT\javaCode\news-community-cloud\report-service\src\main\java\com\guat\cloud\report"

# ---- report Java 文件清单（按分组看，理解每一组在干嘛） ----
$javaFiles = @(
# Controller：对外接口
    "controller\ReportController.java",
    "controller\AdminReportController.java",
    # Service 接口 + 实现
    "service\AdminReportService.java",
    "service\ReportService.java",
    "service\NotificationService.java",
    "service\impl\AdminReportServiceImpl.java",
    "service\impl\ReportServiceImpl.java",
    "service\impl\NotificationServiceImpl.java",
    # Mapper：操作举报域的表
    "mapper\NotificationMapper.java",
    "mapper\ReportMapper.java",
    # 跨域最小复制：查帖子和评论信息要用 news 表 comment 表（过渡态）
    "mapper\NewsMapper.java",
    "mapper\CommentMapper.java",
    # Entity
    "entity\Notification.java",
    "entity\Report.java",
    "entity\Comment.java",
    "entity\News.java",
    "entity\NewsImage.java",
    # DTO
    "dto\ReportHandleResult.java",
    "dto\PageBean.java",
    "dto\ReportVO.java",
    "dto\PostCardVO.java",
    "dto\Result.java",
    # 配置
    "config\WebConfig.java",
    "config\WebMvcConfig.java",
    # 工具
    "utils\UserContext.java",
    "utils\JwtUtils.java",
    # 公共层（每个服务自备一份）
    "interceptor\LoginInterceptor.java",
    "interceptor\PermissionInterceptor.java",
    "annotation\RequiredRole.java",
    "exception\BusinessException.java",
    "exception\GlobalExceptionHandler.java"
)

Write-Host "==> [1/3] 复制 Java 文件 ($($javaFiles.Count) 个) ..." -ForegroundColor Cyan
foreach ($f in $javaFiles) {
    $rel = Split-Path $f -Parent
    $dstDir = Join-Path $dst $rel
    New-Item -ItemType Directory -Path $dstDir -Force | Out-Null
    Copy-Item (Join-Path $src $f) (Join-Path $dstDir (Split-Path $f -Leaf)) -Force
}

# ---- Mapper XML ----
$xmlSrc = "D:\Mysoft\IT\javaCode\News_CourseDesign\src\main\resources\mapper"
$xmlDst = "D:\Mysoft\IT\javaCode\news-community-cloud\report-service\src\main\resources\mapper"
New-Item -ItemType Directory -Path $xmlDst -Force | Out-Null
$xmlFiles = @(
    "NewsMapper.xml",
    "NewsCommentMapper.xml",
    "ReportMapper.xml",
    "NotificationMapper.xml"
)
Write-Host "==> [2/3] 复制 Mapper XML ($($xmlFiles.Count) 个) ..." -ForegroundColor Cyan
foreach ($x in $xmlFiles) {
    Copy-Item (Join-Path $xmlSrc $x) (Join-Path $xmlDst $x) -Force
}

# ---- 包名替换 ----
Write-Host "==> [3/3] 替换包名 com.guat.mynewsapp -> com.guat.cloud.report ..." -ForegroundColor Cyan
Get-ChildItem $dst -Recurse -Filter *.java | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $c = $c.Replace("com.guat.mynewsapp", "com.guat.cloud.report")
    [System.IO.File]::WriteAllText($_.FullName, $c, (New-Object System.Text.UTF8Encoding $false))
}
Get-ChildItem $xmlDst -Filter *.xml | ForEach-Object {
    $c = [System.IO.File]::ReadAllText($_.FullName)
    $c = $c.Replace("com.guat.mynewsapp", "com.guat.cloud.report")
    [System.IO.File]::WriteAllText($_.FullName, $c, (New-Object System.Text.UTF8Encoding $false))
}

Write-Host ""
Write-Host "完成！Java: $((Get-ChildItem $dst -Recurse -Filter *.java).Count) 个, XML: $((Get-ChildItem $xmlDst -File).Count) 个" -ForegroundColor Green
Write-Host "下一步：给 report-service/pom.xml 补依赖（参考 user-service），然后编译。" -ForegroundColor Yellow
