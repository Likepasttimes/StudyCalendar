#ifndef AppSourceRoot
  #error AppSourceRoot is required
#endif

#ifndef AppOutputDir
  #define AppOutputDir "."
#endif

#ifndef AppVersion
  #define AppVersion "1.4.1"
#endif

#ifndef IconPath
  #define IconPath "."
#endif

#define AppName "学习日历"
#define AppExeName "学习日历.exe"
#define AppIdValue "{{B2C77D59-25E1-4D26-97AA-5145C44F62AB}"

[Setup]
AppId={#AppIdValue}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=学习日历
DefaultDirName={localappdata}\Programs\学习日历
DefaultGroupName=学习日历
UsePreviousAppDir=yes
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir={#AppOutputDir}
OutputBaseFilename=学习日历-Windows-V{#AppVersion}-安装程序
Compression=lzma2/max
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\{#AppExeName}
SetupIconFile={#IconPath}
CloseApplications=yes
RestartApplications=no
AllowNoIcons=yes

[Files]
Source: "{#AppSourceRoot}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\学习日历"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"
Name: "{autodesktop}\学习日历"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"

[Registry]
Root: HKCU; Subkey: "Software\学习日历"; ValueType: string; ValueName: "DataDirectory"; ValueData: "{code:GetDataDirectory}"

[Run]
Filename: "{app}\{#AppExeName}"; Description: "启动学习日历"; Flags: nowait postinstall skipifsilent

[Code]
var
  DataDirectoryPage: TInputDirWizardPage;

function ReadPreviousDataDirectory(): String;
var
  PreviousDirectory: String;
begin
  Result := '';
  if RegQueryStringValue(
    HKCU,
    'Software\学习日历',
    'DataDirectory',
    PreviousDirectory
  ) then
    Result := PreviousDirectory;
end;

function DefaultDataDirectory(): String;
begin
  if DirExists('D:\') then
    Result := 'D:\学习日历数据'
  else
    Result := ExpandConstant('{userdocs}\学习日历数据');
end;

procedure InitializeWizard();
var
  CommandLineDirectory: String;
  PreviousDirectory: String;
begin
  DataDirectoryPage := CreateInputDirPage(
    wpSelectDir,
    '选择数据保存位置',
    '任务、周总结、错误记录和附件将保存在这里。',
    '以后运行新版安装程序升级时，会继续使用此目录，不会覆盖已有数据。',
    False,
    ''
  );
  DataDirectoryPage.Add('数据文件夹：');
  CommandLineDirectory := Trim(ExpandConstant('{param:DATADIR|}'));
  PreviousDirectory := ReadPreviousDataDirectory();
  if CommandLineDirectory <> '' then
    DataDirectoryPage.Values[0] := CommandLineDirectory
  else if PreviousDirectory <> '' then
    DataDirectoryPage.Values[0] := PreviousDirectory
  else
    DataDirectoryPage.Values[0] := DefaultDataDirectory();
end;

function GetDataDirectory(Param: String): String;
begin
  Result := DataDirectoryPage.Values[0];
end;

function NormalizeDirectory(Value: String): String;
begin
  Result := Trim(Value);
  while (Length(Result) > 3) and
    ((Result[Length(Result)] = '\') or (Result[Length(Result)] = '/')) do
    Delete(Result, Length(Result), 1);
end;

function HexDigit(Value: Integer): String;
begin
  Result := Copy('0123456789ABCDEF', Value + 1, 1);
end;

function Hex4(Value: Integer): String;
begin
  Result := HexDigit((Value shr 12) and 15) + HexDigit((Value shr 8) and 15) + HexDigit((Value shr 4) and 15) + HexDigit(Value and 15);
end;

function EncodeConfigValue(Value: String): String;
var
  CharacterCode: Integer;
  Index: Integer;
begin
  Result := '';
  for Index := 1 to Length(Value) do
  begin
    CharacterCode := Ord(Value[Index]);
    if (CharacterCode >= 32) and (CharacterCode <= 126) then
      Result := Result + Value[Index]
    else
      Result := Result + '\u' + Hex4(CharacterCode);
  end;
end;

function NextButtonClick(CurPageID: Integer): Boolean;
var
  ProgramDirectoryWithSeparator: String;
  DataDirectoryWithSeparator: String;
  SelectedDataDirectory: String;
begin
  Result := True;
  if (DataDirectoryPage <> nil) and (CurPageID = DataDirectoryPage.ID) then
  begin
    SelectedDataDirectory := NormalizeDirectory(DataDirectoryPage.Values[0]);
    if SelectedDataDirectory = '' then
    begin
      MsgBox('请选择数据保存位置。', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    if Pos(':', SelectedDataDirectory) = 0 then
    begin
      MsgBox('请选择完整的本地磁盘路径。', mbError, MB_OK);
      Result := False;
      Exit;
    end;
    ProgramDirectoryWithSeparator := AddBackslash(
      Uppercase(NormalizeDirectory(WizardDirValue))
    );
    DataDirectoryWithSeparator := AddBackslash(
      Uppercase(SelectedDataDirectory)
    );
    if Pos(
      ProgramDirectoryWithSeparator,
      DataDirectoryWithSeparator
    ) = 1 then
    begin
      MsgBox(
        '数据目录不能放在程序安装目录中，请选择其他文件夹。',
        mbError,
        MB_OK
      );
      Result := False;
      Exit;
    end;
    DataDirectoryPage.Values[0] := SelectedDataDirectory;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
var
  DataDirectory: String;
  ConfigDirectory: String;
  ConfigFile: String;
  ConfigContent: String;
begin
  if CurStep = ssPostInstall then
  begin
    DataDirectory := NormalizeDirectory(DataDirectoryPage.Values[0]);
    ForceDirectories(DataDirectory);
    ConfigDirectory := ExpandConstant('{localappdata}\LearningCalendar');
    ForceDirectories(ConfigDirectory);
    ConfigFile := AddBackslash(ConfigDirectory) + 'data-location.properties';
    ConfigContent := 'data.root=' + EncodeConfigValue(DataDirectory) + #13#10;
    SaveStringToFile(ConfigFile, ConfigContent, False);
  end;
end;
