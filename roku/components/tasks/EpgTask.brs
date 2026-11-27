' Guía de programación. XMLTV (si la lista trae una guía que quepa en memoria) o la guía corta de Xtream.
' Las pantallas piden: request = {reqId, keys: [...]} y reciben result = {reqId, data: {key: [ {s, e, t, d} ]}}.
sub init()
    m.top.functionName = "service"
end sub

sub service()
    port = CreateObject("roMessagePort")
    m.top.observeField("config", port)
    m.top.observeField("request", port)
    m.mode = "none"
    m.data = {}
    m.fetched = {}
    while true
        msg = wait(0, port)
        if type(msg) = "roSGNodeEvent" then
            f = msg.getField()
            if f = "config" then
                configure(msg.getData())
            else if f = "request" then
                answer(msg.getData())
            end if
        end if
    end while
end sub

sub configure(c as object)
    m.data = {}
    m.fetched = {}
    m.mode = "none"
    if c = invalid or c.mode = invalid then
        m.top.state = "none"
        return
    end if
    m.cfg = c
    if c.mode = "xtream" then
        m.mode = "xtream"
        m.top.state = "ready"
    else if c.mode = "xmltv" then
        m.top.state = "loading"
        if loadXmltv(c.url) then
            m.mode = "xmltv"
            m.top.state = "ready"
        else
            m.top.state = "none"
        end if
    else
        m.top.state = "none"
    end if
end sub

function httpGetFile(url as string, path as string, timeoutMs as integer) as boolean
    xfer = CreateObject("roUrlTransfer")
    port = CreateObject("roMessagePort")
    xfer.SetMessagePort(port)
    xfer.SetUrl(url)
    xfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    xfer.InitClientCertificates()
    xfer.AddHeader("User-Agent", userAgent())
    xfer.EnableEncodings(true)
    if not xfer.AsyncGetToFile(path) then return false
    msg = wait(timeoutMs, port)
    if msg = invalid then
        xfer.AsyncCancel()
        return false
    end if
    return msg.GetResponseCode() = 200
end function

function httpGetString(url as string, timeoutMs as integer) as dynamic
    xfer = CreateObject("roUrlTransfer")
    port = CreateObject("roMessagePort")
    xfer.SetMessagePort(port)
    xfer.SetUrl(url)
    xfer.SetCertificatesFile("common:/certs/ca-bundle.crt")
    xfer.InitClientCertificates()
    xfer.AddHeader("User-Agent", userAgent())
    xfer.EnableEncodings(true)
    if not xfer.AsyncGetToString() then return invalid
    msg = wait(timeoutMs, port)
    if msg = invalid then
        xfer.AsyncCancel()
        return invalid
    end if
    if msg.GetResponseCode() <> 200 then return invalid
    return msg.GetString()
end function

function xmltvTime(s as string) as integer
    t = s.Trim()
    if Len(t) < 14 then return 0
    iso = Mid(t, 1, 4) + "-" + Mid(t, 5, 2) + "-" + Mid(t, 7, 2) + "T" + Mid(t, 9, 2) + ":" + Mid(t, 11, 2) + ":" + Mid(t, 13, 2) + "Z"
    d = CreateObject("roDateTime")
    d.FromISO8601String(iso)
    sec = d.AsSeconds()
    rest = Mid(t, 15).Trim()
    if Len(rest) >= 5 then
        sign = Left(rest, 1)
        off = Int(Val(Mid(rest, 2, 2))) * 3600 + Int(Val(Mid(rest, 4, 2))) * 60
        if sign = "+" then sec = sec - off else sec = sec + off
    end if
    return sec
end function

function attrOf(tag as string, name as string) as string
    k = name + "=" + Chr(34)
    p = Instr(1, tag, k)
    if p = 0 then return ""
    e = Instr(p + Len(k), tag, Chr(34))
    if e = 0 then return ""
    return Mid(tag, p + Len(k), e - p - Len(k))
end function

function xmlText(body as string, tag as string) as string
    p = Instr(1, body, "<" + tag)
    if p = 0 then return ""
    s = Instr(p, body, ">")
    e = Instr(s, body, "</" + tag + ">")
    if s = 0 or e = 0 then return ""
    t = Mid(body, s + 1, e - s - 1)
    return t.Replace("&amp;", "&").Replace("&quot;", Chr(34)).Replace("&apos;", "'").Replace("&lt;", "<").Replace("&gt;", ">").Trim()
end function

function loadXmltv(url as string) as boolean
    l = LCase(url)
    if Right(l, 3) = ".gz" then return false
    path = "tmp:/fitotv_epg.xml"
    fs = CreateObject("roFileSystem")
    fs.Delete(path)
    if not httpGetFile(url, path, 90000) then return false
    st = fs.Stat(path)
    if st = invalid or st.size > 30000000 then
        fs.Delete(path)
        return false
    end if
    s = ReadAsciiFile(path)
    fs.Delete(path)
    now = nowSec()
    from = now - 3600
    upto = now + 30 * 3600
    chunks = s.Split("</programme>")
    s = invalid
    for each ch in chunks
        p = Instr(1, ch, "<programme")
        if p > 0 then
            te = Instr(p, ch, ">")
            tag = Mid(ch, p, te - p)
            st0 = xmltvTime(attrOf(tag, "start"))
            st1 = xmltvTime(attrOf(tag, "stop"))
            if st1 > from and st0 < upto and st0 > 0 then
                key = LCase(attrOf(tag, "channel"))
                body = Mid(ch, te + 1)
                title = xmlText(body, "title")
                if title = "" then title = "Sin título"
                desc = Left(xmlText(body, "desc"), 300)
                if not m.data.DoesExist(key) then m.data[key] = []
                m.data[key].Push({s: st0, e: st1, t: title, d: desc})
            end if
        end if
    end for
    for each k in m.data
        m.data[k].SortBy("s")
    end for
    return m.data.Count() > 0
end function

function b64(s as dynamic) as string
    if s = invalid or s = "" then return ""
    ba = CreateObject("roByteArray")
    ba.FromBase64String(s)
    return ba.ToAsciiString()
end function

sub fetchXtream(key as string)
    if key = "" or m.fetched.DoesExist(key) then return
    m.fetched[key] = true
    raw = httpGetString(m.cfg.api + "&action=get_short_epg&stream_id=" + key + "&limit=12", 15000)
    if raw = invalid then return
    j = ParseJson(raw)
    if j = invalid or j.epg_listings = invalid then return
    out = []
    for each e in j.epg_listings
        s0 = numOf(e.start_timestamp)
        s1 = numOf(e.stop_timestamp)
        if s1 > 0 then out.Push({s: s0, e: s1, t: b64(e.title), d: Left(b64(e.description), 300)})
    end for
    out.SortBy("s")
    m.data[key] = out
end sub

sub answer(req as object)
    if req = invalid or req.keys = invalid then return
    data = {}
    n = 0
    for each k in req.keys
        key = LCase(k)
        if m.mode = "xtream" then
            fetchXtream(key)
        end if
        if m.data.DoesExist(key) then data[key] = m.data[key]
        n = n + 1
        if m.mode = "xtream" and n mod 6 = 0 then m.top.result = {reqId: req.reqId, data: data, partial: true}
    end for
    m.top.result = {reqId: req.reqId, data: data}
end sub
