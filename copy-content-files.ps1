# ============================================================
#  作业脚本：把单体 content 域代码复制到 content-service
#  使用方法：右键"使用 PowerShell 运行"，或：
#    powershell -ExecutionPolicy Bypass -File copy-content-files.ps1
#  执行完后会自动把包名 com.guat.mynewsapp 换成 com.guat.cloud.content
# ============================================================
$ErrorActionPreference = "Stop"

$src = "D:\Mysoft\IT\javaCode\News_CourseDesign\src\main\java\com\guat\mynewsapp"
$dst = "D:\Mysoft\IT\javaCode\news-community-cloud\content-service\src\main\java\com\guat\cloud\content"

# ---- 内容域 Java 文件清单（按分组看，理解每一组在干嘛） ----
$javaFiles = @(
  # Controller：对外接口
  "controller\NewsController.java",
  "controller\CommentController.java",
  "controller\PostDisinterestController.java",
  "controller\RedisTestController.java",   # 测试用，可选
  # Service 接口 + 实现
  "service\NewsService.java",
  "service\CommentService.java",
  "service\PostDisinterestService.java",
  "service\impl\NewsServiceImpl.java",
  "service\impl\CommentServiceImpl.java",
  "service\impl\PostDisinterestServiceImpl.java",
  # Mapper：操作内容域的表
  "mapper\NewsMapper.java",
  "mapper\NewsImageMapper.java",
  "mapper\NewsLikeMapper.java",
  "mapper\CommentMapper.java",
  "mapper\CommentLikeMapper.java",
  "mapper\CommentDislikeMapper.java",
  "mapper\PostLikeMapper.java",
  "mapper\PostDisinterestMapper.java",
  # 跨域最小复制：查作者信息要用 user 表（过渡态）
  "mapper\UserMapper.java",
  # Entity
  "entity\News.java",
  "entity\NewsImage.java",
  "entity\NewsLike.java",
  "entity\Comment.java",
  "entity\CommentLike.java",
  "entity\CommentDislike.java",
  "entity\PostLike.java",
  "entity\PostDisinterest.java",
  "entity\User.java",
  # DTO
  "dto\NewsDTO.java",
  "dto\NewsEditDTO.java",
  "dto\MediaKeepDTO.java",
  "dto\PostCardVO.java",
  "dto\CommentCardVO.java",
  "dto\PageBean.java",
  "dto\Result.java",
  # 定时任务：点赞缓存回写 MySQL
  "task\LikeSyncTask.java",
  "task\CommentLikeSyncTask.java",
  # 配置
  "config\RedisLikeWarmUp.java",
  "config\RedisConfig.java",
  "config\WebConfig.java",
  "config\WebMvcConfig.java",
  # 工具
  "utils\RedisKeyConstants.java",
  "utils\JwtUtils.java",
  "utils\UserContext.java",
  "utils\FileUploadUtil.java",
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
$xmlDst = "D:\Mysoft\IT\javaCode\news-community-cloud\content-service\src\main\resources\mapper"
New-Item -ItemType Directory -Path $xmlDst -Force | Out-Null
$xmlFiles = @(
  "NewsMapper.xml",
  "NewsImageMapper.xml",
  "NewsCommentMapper.xml",
  "CommentLikeMapper.xml",
  "CommentDisLikeMapper.xml",
  "PostLikeMapper.xml",
  "PostDisinterestMapper.xml",
  "UserMapper.xml"    # 跨域查作者
)
Write-Host "==> [2/3] 复制 Mapper XML ($($xmlFiles.Count) 个) ..." -ForegroundColor Cyan
foreach ($x in $xmlFiles) {
  Copy-Item (Join-Path $xmlSrc $x) (Join-Path $xmlDst $x) -Force
}

# ---- 包名替换 ----
Write-Host "==> [3/3] 替换包名 com.guat.mynewsapp -> com.guat.cloud.content ..." -ForegroundColor Cyan
Get-ChildItem $dst -Recurse -Filter *.java | ForEach-Object {
  $c = [System.IO.File]::ReadAllText($_.FullName)
  $c = $c.Replace("com.guat.mynewsapp", "com.guat.cloud.content")
  [System.IO.File]::WriteAllText($_.FullName, $c, (New-Object System.Text.UTF8Encoding $false))
}
Get-ChildItem $xmlDst -Filter *.xml | ForEach-Object {
  $c = [System.IO.File]::ReadAllText($_.FullName)
  $c = $c.Replace("com.guat.mynewsapp", "com.guat.cloud.content")
  [System.IO.File]::WriteAllText($_.FullName, $c, (New-Object System.Text.UTF8Encoding $false))
}

Write-Host ""
Write-Host "完成！Java: $((Get-ChildItem $dst -Recurse -Filter *.java).Count) 个, XML: $((Get-ChildItem $xmlDst -File).Count) 个" -ForegroundColor Green
Write-Host "下一步：给 content-service/pom.xml 补依赖（参考 user-service），然后编译。" -ForegroundColor Yellow
