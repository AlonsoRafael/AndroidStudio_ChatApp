# Script para resolver problemas de conexão em múltiplos emuladores - ChatApp
# Execute este script no PowerShell como Administrador

Write-Host "🚀 Configurando ambiente para múltiplos emuladores Android..." -ForegroundColor Green

# Verificar se Android SDK está no PATH
$androidSDK = $env:ANDROID_HOME
if (-not $androidSDK) {
    Write-Host "❌ ANDROID_HOME não está definido. Configure o Android SDK primeiro." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Android SDK encontrado em: $androidSDK" -ForegroundColor Green

# Verificar ADB
$adbPath = "$androidSDK\platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    Write-Host "❌ ADB não encontrado. Verifique a instalação do Android SDK." -ForegroundColor Red
    exit 1
}

Write-Host "✅ ADB encontrado" -ForegroundColor Green

# Função para matar processos do emulador existentes
function Stop-Emulators {
    Write-Host "🔄 Parando emuladores existentes..." -ForegroundColor Yellow
    & $adbPath kill-server
    Start-Sleep -Seconds 2
    & $adbPath start-server
    
    # Matar processos do emulador
    Get-Process | Where-Object {$_.ProcessName -like "*emulator*"} | Stop-Process -Force -ErrorAction SilentlyContinue
    Get-Process | Where-Object {$_.ProcessName -like "*qemu*"} | Stop-Process -Force -ErrorAction SilentlyContinue
}

# Função para configurar DNS nos emuladores
function Set-EmulatorDNS {
    param($emulatorPort)
    
    Write-Host "🌐 Configurando DNS para emulador na porta $emulatorPort..." -ForegroundColor Cyan
    
    # Aguardar emulador estar pronto
    Start-Sleep -Seconds 10
    
    # Configurar DNS
    & $adbPath -s "emulator-$emulatorPort" shell "settings put global captive_portal_server www.google.com"
    & $adbPath -s "emulator-$emulatorPort" shell "settings put global captive_portal_use_https 0"
    & $adbPath -s "emulator-$emulatorPort" shell "settings put global http_proxy :0"
    
    # Testar conectividade
    Write-Host "🔍 Testando conectividade do emulador $emulatorPort..." -ForegroundColor Yellow
    & $adbPath -s "emulator-$emulatorPort" shell "ping -c 1 8.8.8.8"
}

# Parar emuladores existentes
Stop-Emulators

# Limpar projeto
Write-Host "🧹 Limpando projeto..." -ForegroundColor Yellow
if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat clean
} else {
    Write-Host "⚠️ gradlew.bat não encontrado. Execute manualmente: ./gradlew clean" -ForegroundColor Yellow
}

# Construir projeto
Write-Host "🔨 Construindo projeto..." -ForegroundColor Yellow
if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat build
} else {
    Write-Host "⚠️ Execute manualmente: ./gradlew build" -ForegroundColor Yellow
}

# Verificar se o APK foi gerado
$apkPath = ".\app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "❌ APK não encontrado. Build falhou." -ForegroundColor Red
    exit 1
}

Write-Host "✅ APK gerado com sucesso" -ForegroundColor Green

# Solicitar nome dos AVDs ao usuário
Write-Host "📱 Configure seus AVDs..." -ForegroundColor Cyan
Write-Host "Para listar AVDs disponíveis, execute: $androidSDK\emulator\emulator.exe -list-avds" -ForegroundColor Gray

$avd1 = Read-Host "Digite o nome do primeiro AVD"
$avd2 = Read-Host "Digite o nome do segundo AVD"

if (-not $avd1 -or -not $avd2) {
    Write-Host "❌ Nomes de AVD são obrigatórios." -ForegroundColor Red
    exit 1
}

# Iniciar emuladores
Write-Host "🚀 Iniciando emuladores..." -ForegroundColor Green

$emulatorPath = "$androidSDK\emulator\emulator.exe"

# Emulador 1 - Porta 5554
Write-Host "📱 Iniciando emulador 1: $avd1 na porta 5554..." -ForegroundColor Cyan
Start-Process -FilePath $emulatorPath -ArgumentList "-avd", $avd1, "-port", "5554", "-dns-server", "8.8.8.8,8.8.4.4", "-no-snapshot-load" -WindowStyle Minimized

# Aguardar um pouco antes de iniciar o segundo
Start-Sleep -Seconds 5

# Emulador 2 - Porta 5556  
Write-Host "📱 Iniciando emulador 2: $avd2 na porta 5556..." -ForegroundColor Cyan
Start-Process -FilePath $emulatorPath -ArgumentList "-avd", $avd2, "-port", "5556", "-dns-server", "8.8.8.8,8.8.4.4", "-no-snapshot-load" -WindowStyle Minimized

# Aguardar emuladores iniciarem
Write-Host "⏳ Aguardando emuladores iniciarem (60 segundos)..." -ForegroundColor Yellow
Start-Sleep -Seconds 60

# Verificar dispositivos
Write-Host "🔍 Verificando dispositivos conectados..." -ForegroundColor Yellow
& $adbPath devices

# Configurar DNS nos emuladores
Set-EmulatorDNS -emulatorPort 5554
Set-EmulatorDNS -emulatorPort 5556

# Instalar APK nos emuladores
Write-Host "📦 Instalando APK no emulador 1..." -ForegroundColor Cyan
& $adbPath -s emulator-5554 install -r $apkPath

Write-Host "📦 Instalando APK no emulador 2..." -ForegroundColor Cyan  
& $adbPath -s emulator-5556 install -r $apkPath

Write-Host ""
Write-Host "✅ CONFIGURAÇÃO CONCLUÍDA!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 PRÓXIMOS PASSOS:" -ForegroundColor Cyan
Write-Host "1. Abra o app em ambos os emuladores"
Write-Host "2. Crie contas diferentes em cada emulador:"
Write-Host "   - Emulador 1: usuario1@teste.com"
Write-Host "   - Emulador 2: usuario2@teste.com"
Write-Host "3. Tente adicionar contatos entre os emuladores"
Write-Host ""
Write-Host "🔍 COMANDOS ÚTEIS PARA DEBUG:" -ForegroundColor Yellow
Write-Host "Ver logs Firebase:    adb logcat -s FirebaseDatabase"
Write-Host "Ver logs do app:      adb logcat -s ChatApp"
Write-Host "Ver logs de rede:     adb logcat -s NetworkSecurityConfig"
Write-Host "Limpar cache do app:  adb shell pm clear com.example.chatapp"
Write-Host ""

# Manter o script rodando para mostrar logs
$showLogs = Read-Host "Deseja ver logs em tempo real? (s/n)"
if ($showLogs -eq 's' -or $showLogs -eq 'S') {
    Write-Host "📊 Mostrando logs (Ctrl+C para parar)..." -ForegroundColor Green
    & $adbPath logcat -s ChatApp:* FirebaseDatabase:* NetworkSecurityConfig:*
}
