' Descargas y la API de Xtream (para las tareas en segundo plano).

' Baja un texto a un archivo temporal y lo lee. {ok, text} o {ok: false, message}.
function fetchText(url as string, timeoutMs = 60000 as integer) as object
    xfer = CreateObject("roUrlTransfer")
    port = CreateObject("roMessagePort")
    xfer.SetMessagePort(port)
    xfer.SetUrl(url)
    xfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    xfer.InitClientCertificates()
    xfer.AddHeader("User-Agent", userAgent())
    xfer.EnableEncodings(true)
    xfer.RetainBodyOnError(true)
    tmp = "tmp:/fitotv_" + newId() + ".txt"
    fs = CreateObject("roFileSystem")
    if not xfer.AsyncGetToFile(tmp) then return {ok: false, message: "Esa dirección no parece un enlace válido"}
    msg = wait(timeoutMs, port)
    if msg = invalid then
        xfer.AsyncCancel()
        fs.Delete(tmp)
        return {ok: false, message: "El servidor de la lista tardó demasiado en responder"}
    end if
    code = msg.GetResponseCode()
    if code = 200 then
        txt = ReadAsciiFile(tmp)
        fs.Delete(tmp)
        return {ok: true, text: txt}
    end if
    fs.Delete(tmp)
    if code = 401 or code = 403 or code = 884 then return {ok: false, message: "El proveedor no dejó entrar. Revisa el usuario y la contraseña, o si la cuenta venció"}
    if code = 404 then return {ok: false, message: "Esa dirección no existe en el servidor"}
    if code < 0 then
        reason = LCase(msg.GetFailureReason())
        if Instr(1, reason, "resolve") > 0 then return {ok: false, message: "No encontramos esa dirección. Revisa que esté bien escrita y que la tele tenga internet", offline: true}
        if Instr(1, reason, "timed out") > 0 then return {ok: false, message: "El servidor de la lista tardó demasiado en responder", offline: true}
        return {ok: false, message: "No pudimos conectarnos con el servidor de la lista", offline: true}
    end if
    return {ok: false, message: "El servidor respondió con un error (" + code.ToStr() + ")"}
end function

' Texto de un valor JSON que puede venir como número o como texto.
function anyStr(v as dynamic) as string
    t = type(v)
    if t = "String" or t = "roString" then return v
    if t = "Integer" or t = "roInteger" or t = "LongInteger" then return v.ToStr()
    if t = "Float" or t = "roFloat" or t = "Double" or t = "roDouble" then return Int(v).ToStr()
    if t = "Boolean" or t = "roBoolean" then
        if v then return "1" else return "0"
    end if
    return ""
end function

' "http://srv:8080/get.php?username=a&password=b&type=m3u_plus" → {server, user, pass}
function xtCredsFromUrl(url as string) as dynamic
    u = url.Trim()
    q = Instr(1, u, "?")
    if q = 0 then return invalid
    path = LCase(Left(u, q - 1))
    if Right(path, 8) <> "/get.php" and Right(path, 15) <> "/player_api.php" then return invalid
    params = parseForm(Mid(u, q + 1))
    if params.username = invalid or params.password = invalid then return invalid
    slash = 0
    p = Left(u, q - 1)
    for i = Len(p) to 1 step -1
        if Mid(p, i, 1) = "/" then
            slash = i
            exit for
        end if
    end for
    return {server: Left(p, slash - 1), user: params.username, pass: params.password}
end function

function xtUrl(c as object, action as string, extra = "" as string) as string
    u = xtreamApi(c.server, c.user, c.pass)
    if action <> "" then u = u + "&action=" + action
    return u + extra
end function

' Un arreglo JSON grande partido en sus objetos (texto). Quien llama hace ParseJson("{" + parte + "}")
' de uno en uno: así no se arma el árbol completo en memoria.
function xtParts(text as string) as object
    t = text.Trim()
    if Left(t, 1) <> "[" then return []
    inner = Mid(t, 2, Len(t) - 2).Trim()
    if Len(inner) < 2 then return []
    inner = Mid(inner, 2, Len(inner) - 2)
    re = CreateObject("roRegex", "\}\s*,\s*\{", "")
    return re.Split(inner)
end function

function xtObj(part as string) as dynamic
    o = ParseJson("{" + part + "}")
    if type(o) = "roAssociativeArray" then return o
    return invalid
end function
