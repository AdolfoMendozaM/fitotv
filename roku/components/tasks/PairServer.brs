' Servidorcito dentro del Roku: el celular (en el mismo Wi-Fi) abre la dirección del QR,
' pega su lista y el Roku la recibe. No pasa por internet.
sub init()
    m.top.functionName = "serve"
end sub

function localIp() as string
    di = CreateObject("roDeviceInfo")
    ips = di.GetIPAddrs()
    for each k in ips
        ip = ips[k]
        if ip <> invalid and ip <> "" then return ip
    end for
    return ""
end function

sub serve()
    port = CreateObject("roMessagePort")
    m.top.observeField("stopNow", port)
    sock = invalid
    used = 0
    for p = 8090 to 8095
        s = CreateObject("roStreamSocket")
        s.setMessagePort(port)
        addr = CreateObject("roSocketAddress")
        addr.setPort(p)
        s.setAddress(addr)
        s.notifyReadable(true)
        if s.listen(8) then
            sock = s
            used = p
            exit for
        end if
        s.close()
    end for
    ip = localIp()
    if sock = invalid or ip = "" then
        m.top.address = ""
        return
    end if
    m.top.address = "http://" + ip + ":" + used.ToStr()
    m.page = ReadAsciiFile("pkg:/source/pair.html")
    conns = {}
    while true
        msg = wait(0, port)
        t = type(msg)
        if t = "roSGNodeEvent" then
            if msg.getField() = "stopNow" and msg.getData() = true then exit while
        else if t = "roSocketEvent" then
            id = msg.getSocketID()
            if id = sock.getID() then
                if sock.isReadable() then
                    c = sock.accept()
                    if c <> invalid then
                        c.setMessagePort(port)
                        c.notifyReadable(true)
                        conns[c.getID().ToStr()] = {s: c, buf: CreateObject("roByteArray"), headLen: -1, need: 0}
                    end if
                end if
            else
                key = id.ToStr()
                cn = conns[key]
                if cn <> invalid then
                    if readFrom(cn) then
                        if requestDone(cn) then
                            handle(cn)
                            cn.s.close()
                            conns.Delete(key)
                        end if
                    else
                        cn.s.close()
                        conns.Delete(key)
                    end if
                end if
            end if
        end if
    end while
    for each k in conns
        conns[k].s.close()
    end for
    sock.close()
end sub

' Lee lo que haya; false si el celular cerró la conexión.
function readFrom(cn as object) as boolean
    if not cn.s.isReadable() then return true
    chunk = CreateObject("roByteArray")
    chunk[16383] = 0
    n = cn.s.receive(chunk, 0, 16384)
    if n <= 0 then return false
    if n = 16384 then
        cn.buf.Append(chunk)
    else
        for i = 0 to n - 1
            cn.buf.Push(chunk[i])
        end for
    end if
    return true
end function

function requestDone(cn as object) as boolean
    b = cn.buf
    if cn.headLen < 0 then
        limit = b.Count() - 4
        if limit > 65536 then limit = 65536
        for i = 0 to limit
            if b[i] = 13 and b[i + 1] = 10 and b[i + 2] = 13 and b[i + 3] = 10 then
                cn.headLen = i + 4
                exit for
            end if
        end for
        if cn.headLen < 0 then return false
        head = LCase(headerText(b, cn.headLen))
        cn.need = 0
        p = Instr(1, head, "content-length:")
        if p > 0 then
            e = Instr(p, head, Chr(13))
            cn.need = Int(Val(Mid(head, p + 15, e - p - 15).Trim()))
        end if
    end if
    return b.Count() >= cn.headLen + cn.need
end function

function headerText(b as object, n as integer) as string
    h = CreateObject("roByteArray")
    for i = 0 to n - 1
        h.Push(b[i])
    end for
    return h.ToAsciiString()
end function

sub handle(cn as object)
    head = headerText(cn.buf, cn.headLen)
    first = head.Split(Chr(13) + Chr(10))[0]
    parts = first.Split(" ")
    method = parts[0]
    path = "/"
    if parts.Count() > 1 then path = parts[1]
    if method = "GET" and (path = "/" or path = "/index.html") then
        respond(cn.s, 200, m.page)
    else if method = "POST" and path = "/lista" then
        whole = cn.buf.ToAsciiString()
        body = Mid(whole, Len(head) + 1)
        f = parseForm(body)
        respond(cn.s, 200, receive(f))
    else
        respond(cn.s, 404, "No encontrado")
    end if
end sub

function result(ok as boolean, msg as string) as string
    cls = "err"
    if ok then cls = "ok"
    return m.page.Replace("<!--RESULT-->", "<div class=" + Chr(34) + "result " + cls + Chr(34) + ">" + msg + "</div>")
end function

function receive(f as object) as string
    name = ""
    if f.nombre <> invalid then name = Left(f.nombre.Trim(), 40)
    if name = "" then name = "Mi lista"
    tipo = f.tipo
    if tipo = "enlace" then
        if f.enlace = invalid or f.enlace.Trim() = "" then return result(false, "Falta el enlace de la lista.")
        m.top.received = {kind: "URL", name: name, url: f.enlace.Trim()}
    else if tipo = "archivo" then
        txt = f.contenido
        if txt = invalid or txt.Trim() = "" then return result(false, "Falta elegir el archivo.")
        if Instr(1, UCase(txt), "#EXT") = 0 and Instr(1, LCase(txt), "http") = 0 then return result(false, "Ese archivo no parece una lista M3U.")
        m.top.received = {kind: "FILE", name: name, text: txt}
    else if tipo = "usuario" then
        if f.servidor = invalid or f.usuario = invalid or f.contrasena = invalid or f.servidor.Trim() = "" or f.usuario.Trim() = "" or f.contrasena = "" then
            return result(false, "Faltan datos: servidor, usuario y contraseña.")
        end if
        m.top.received = {kind: "XTREAM", name: name, server: f.servidor.Trim(), user: f.usuario.Trim(), pass: f.contrasena}
    else
        return result(false, "Elige cómo quieres agregar tu lista.")
    end if
    return result(true, "Listo. Tu lista " + Chr(34) + escapeHtml(name) + Chr(34) + " ya va en camino a la tele.")
end function

sub respond(s as object, code as integer, body as string)
    status = "200 OK"
    if code = 404 then status = "404 Not Found"
    b = CreateObject("roByteArray")
    b.FromAsciiString(body)
    head = "HTTP/1.1 " + status + Chr(13) + Chr(10) + "Content-Type: text/html; charset=utf-8" + Chr(13) + Chr(10) + "Content-Length: " + b.Count().ToStr() + Chr(13) + Chr(10) + "Connection: close" + Chr(13) + Chr(10) + Chr(13) + Chr(10)
    sendAll(s, head)
    sendBytes(s, b)
end sub

sub sendAll(s as object, txt as string)
    b = CreateObject("roByteArray")
    b.FromAsciiString(txt)
    sendBytes(s, b)
end sub

sub sendBytes(s as object, b as object)
    sent = 0
    total = b.Count()
    tries = 0
    while sent < total and tries < 400
        n = s.send(b, sent, total - sent)
        if n > 0 then
            sent = sent + n
        else
            tries = tries + 1
            sleep(10)
        end if
    end while
end sub
