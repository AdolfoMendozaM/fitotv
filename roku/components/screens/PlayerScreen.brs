' Reproductor a pantalla completa. En vivo: ▲▼ cambia de canal. Películas y series: pausa y adelantar.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    bg = m.top.createChild("Rectangle")
    bg.width = 1920
    bg.height = 1080
    bg.color = "0x000000FF"
    m.video = m.top.createChild("Video")
    m.video.width = 1920
    m.video.height = 1080
    m.video.enableUI = false
    m.video.notificationInterval = 1
    m.video.observeField("state", "onState")
    m.video.observeField("position", "onPosition")
    m.shade = m.top.createChild("Group")
    top = uiImage(m.shade, "pkg:/images/fade_up.png", 0, 0, 1920, 200, "scaleToFill")
    top.opacity = 0.85
    uiImage(m.shade, "pkg:/images/fade_down.png", 0, 560, 1920, 520, "scaleToFill")
    m.banner = m.top.createChild("Group")
    m.loading = m.top.createChild("Group")
    m.errorG = m.top.createChild("Group")
    m.bannerTimer = m.top.createChild("Timer")
    m.bannerTimer.duration = 5
    m.bannerTimer.observeField("fire", "hideBanner")
    m.zapTimer = m.top.createChild("Timer")
    m.zapTimer.duration = 0.55
    m.zapTimer.observeField("fire", "onZapTimer")
    m.retryTimer = m.top.createChild("Timer")
    m.retryTimer.duration = 1.5
    m.retryTimer.observeField("fire", "retry")
    m.saveTimer = m.top.createChild("Timer")
    m.saveTimer.duration = 15
    m.saveTimer.repeat = true
    m.saveTimer.observeField("fire", "savePosition")
    m.epgProgs = invalid
    m.reqN = 0
    m.global.epg.observeFieldScoped("result", "onEpg")
end sub

sub onActiveChange()
    if m.top.active then
        if m.ch = invalid then
            p = m.top.params
            m.zap = p.zap
            m.resume = 0
            if p.resume <> invalid then m.resume = p.resume
            st = app("getState")
            m.profile = st.profile
            m.list = st.activeList
            startChannel(p.id)
        end if
        m.top.setFocus(true)
    else
        savePosition()
        m.saveTimer.control = "stop"
        m.zapTimer.control = "stop"
        m.retryTimer.control = "stop"
        m.video.control = "stop"
    end if
end sub

' ------------------------------------------------------------------ reproducir
sub startChannel(id as string)
    n = app("channelNode", {id: id})
    if n = invalid then return
    m.ch = n
    m.isVod = n.kind <> "LIVE"
    m.retries = 0
    m.pendingId = invalid
    m.epgProgs = invalid
    m.paused = false
    uiClear(m.errorG)
    v = CreateObject("roSGNode", "ContentNode")
    v.url = n.url
    v.streamFormat = n.streamFormat
    v.title = n.title
    v.live = not m.isVod
    if m.isVod and m.resume > 5 then v.PlayStart = m.resume
    m.resume = 0
    ha = CreateObject("roHttpAgent")
    ha.SetCertificatesFile("common:/certs/ca-bundle.crt")
    ha.InitClientCertificates()
    ua = userAgent()
    if n.ua <> invalid and n.ua <> "" then ua = n.ua
    ha.AddHeader("User-Agent", ua)
    if n.ref <> invalid and n.ref <> "" then ha.AddHeader("Referer", n.ref)
    m.video.setHttpAgent(ha)
    m.video.content = v
    m.video.control = "play"
    showLoading(true)
    if m.isVod then
        m.saveTimer.control = "start"
    else
        m.saveTimer.control = "stop"
        requestEpg(n)
    end if
    showBanner()
end sub

sub retry()
    if m.ch = invalid then return
    uiClear(m.errorG)
    showLoading(true)
    m.video.control = "stop"
    m.video.control = "play"
end sub

sub onState()
    s = m.video.state
    if s = "playing" then
        showLoading(false)
        uiClear(m.errorG)
        m.retries = 0
        m.paused = false
        if m.isVod then drawBanner()
    else if s = "buffering" then
        showLoading(true)
    else if s = "paused" then
        m.paused = true
        if m.isVod then showBanner()
    else if s = "error" then
        showLoading(false)
        if not m.isVod and m.retries < 1 then
            m.retries = m.retries + 1
            m.retryTimer.control = "start"
            return
        end if
        showError()
    else if s = "finished" then
        if m.isVod then
            app("forgetRecent", {id: m.ch.id})
            m.ch = invalid
            app("back")
        else
            retry()
        end if
    end if
end sub

sub showLoading(on as boolean)
    uiClear(m.loading)
    if on then uiSpinner(m.loading, 912, 492, 96, m.c.menta)
end sub

function errorText() as string
    code = m.video.errorCode
    if code = -1 then return "El canal no respondió. Puede estar caído o la cuenta venció."
    if code = 0 or code = -2 then return "No pudimos conectarnos con el canal."
    if code = -5 then return "Este formato de video no se puede reproducir en esta tele."
    if code = -4 then return "Este canal no está disponible ahora."
    return "Se perdió la señal un momento."
end function

sub showError()
    c = m.c
    uiClear(m.errorG)
    uiBox(m.errorG, 0, 0, 1920, 1080, "0x0A1747D9", 0)
    g = m.errorG.createChild("Group")
    g.translation = [460, 330]
    uiCircle(g, 452, 0, 96, "0xFFC53D33")
    uiIcon(g, "warn", 476, 24, 48, c.ambar)
    uiLabel(g, errorText(), 0, 130, 45, c.text, "display", 1000, 2, "center")
    hint = "Presiona OK para intentar otra vez o cambia de canal con ▲▼."
    if m.isVod then hint = "Presiona OK para intentar otra vez o Atrás para salir."
    uiLabel(g, hint, 0, 270, 27, c.muted2, "regular", 1000, 2, "center")
    uiLabel(g, m.ch.title, 0, 340, 23, c.dim, "medium", 1000, 1, "center")
end sub

' ------------------------------------------------------------------ guía del canal
sub requestEpg(n as object)
    k = ""
    cfg = m.global.epg.config
    if cfg <> invalid and cfg.mode = "xtream" and n.sid <> invalid and n.sid <> "" then
        k = n.sid
    else if n.tvgId <> invalid then
        k = LCase(n.tvgId)
    end if
    m.epgKey = k
    if k = "" then return
    m.reqN = m.reqN + 1
    m.global.epg.request = {reqId: "player" + m.reqN.ToStr(), keys: [k]}
end sub

sub onEpg(ev as object)
    r = ev.getData()
    if r = invalid or r.reqId = invalid or Left(r.reqId, 6) <> "player" then return
    if m.epgKey <> invalid and r.data.DoesExist(m.epgKey) then
        m.epgProgs = r.data[m.epgKey]
        if m.banner.visible then drawBanner()
    end if
end sub

function nowNext() as object
    out = {now: invalid, next: invalid}
    if m.epgProgs = invalid then return out
    t = nowSec()
    for each p in m.epgProgs
        if p.s <= t and p.e > t then
            out.now = p
        else if p.s > t and out.next = invalid then
            out.next = p
        end if
    end for
    return out
end function

' ------------------------------------------------------------------ cintillo
sub showBanner()
    m.banner.visible = true
    m.shade.visible = true
    drawBanner()
    m.bannerTimer.control = "stop"
    if not (m.isVod and m.paused) then m.bannerTimer.control = "start"
end sub

sub hideBanner()
    if m.isVod and m.paused then return
    m.banner.visible = false
    m.shade.visible = false
end sub

function keycap(parent as object, x as float, y as float, label as string, icon as string, text as string) as float
    c = m.c
    w = 54
    if label <> "" then
        probe = uiLabel(parent, label, 0, 0, 20, c.text, "bold")
        w = probe.boundingRect().width + 28
        parent.removeChild(probe)
        if w < 54 then w = 54
    end if
    uiBox(parent, x, y, w, 42, "0x22397FFF", 10)
    if icon <> "" then
        uiIcon(parent, icon, x + (w - 26) / 2, y + 8, 26, c.text)
    else
        uiLabel(parent, label, x, y + 9, 20, c.text, "bold", w, 1, "center")
    end if
    lb = uiLabel(parent, text, x + w + 12, y + 8, 23, c.muted2, "medium")
    return x + w + 12 + lb.boundingRect().width + 30
end function

sub drawBanner()
    uiClear(m.banner)
    if m.ch = invalid then return
    c = m.c
    ch = m.ch
    if m.pendingId <> invalid then ch = app("channelNode", {id: m.pendingId})
    if ch = invalid then return
    if not m.isVod then
        ' quién está viendo
        if m.profile <> invalid then
            top = m.banner.createChild("Group")
            top.translation = [84, 60]
            pname = m.profile.name
            lname = ""
            if m.list <> invalid then lname = m.list.name
            probe = uiLabel(top, pname, 0, 0, 25, c.text, "semibold")
            w1 = probe.boundingRect().width
            top.removeChild(probe)
            probe = uiLabel(top, lname, 0, 0, 25, c.muted2)
            w2 = probe.boundingRect().width
            top.removeChild(probe)
            uiPill(top, 0, 0, 54 + 18 + w1 + 34 + w2 + 28, 72, "0x12235FE6")
            uiAvatar(top, m.profile.avatar, 9, 9, 54)
            uiLabel(top, pname, 81, 20, 25, c.text, "semibold")
            uiCircle(top, 81 + w1 + 14, 33, 6, "0x5C6FB0FF")
            uiLabel(top, lname, 81 + w1 + 34, 20, 25, c.muted2)
        end if
        g = m.banner.createChild("Group")
        g.translation = [84, 700]
        uiPill(g, 0, 0, 156, 48, "0x12235FE6")
        uiCircle(g, 18, 18, 12, c.menta)
        uiLabel(g, "En vivo", 40, 10, 23, c.text, "semibold")
        x = 0
        if ch.num <> invalid and ch.num > 0 then
            nl = uiLabel(g, ch.num.ToStr(), 0, 84, 90, c.num, "display")
            x = nl.boundingRect().width + 27
        end if
        uiLogoPlate(g, ch.title, ch.HDPosterUrl, x, 96, 156, 90, 20)
        tx = x + 156 + 27
        uiLabel(g, ch.title, tx, 76, 27, c.muted2, "medium", 1100)
        nn = {now: invalid, next: invalid}
        if m.pendingId = invalid then nn = nowNext()
        title = "En vivo"
        if nn.now <> invalid then title = nn.now.t
        uiLabel(g, title, tx, 110, 54, c.text, "display", 1752 - tx - 84)
        if nn.now <> invalid then
            p = nn.now
            tl = uiLabel(g, hhmm(p.s) + " a " + hhmm(p.e), tx, 186, 24, c.muted2)
            bx = tx + tl.boundingRect().width + 18
            frac = (nowSec() - p.s) / (p.e - p.s)
            uiPill(g, bx, 196, 300, 8, c.track)
            uiPill(g, bx, 196, 300 * frac + 8, 8, c.menta)
            mins = Int((p.e - nowSec()) / 60)
            uiLabel(g, "quedan " + mins.ToStr() + " min", bx + 318, 186, 24, c.muted2)
        end if
        if nn.next <> invalid then uiLabel(m.banner, "Después, a las " + hhmm(nn.next.s) + ": " + nn.next.t, 936, 900, 24, c.muted, "regular", 900, 1, "right")
        hints = m.banner.createChild("Group")
        hx = keycap(hints, 0, 0, "", "updown", "Cambiar de canal")
        hx = keycap(hints, hx, 0, "", "back", "Salir")
        hx = keycap(hints, hx, 0, "OK", "", "Ocultar")
        hx = keycap(hints, hx, 0, "*", "", "Favorito")
        hints.translation = [1836 - hx + 30, 960]
    else
        g = m.banner.createChild("Group")
        g.translation = [84, 760]
        kind = "Película"
        if ch.kind = "SERIES" then kind = "Serie"
        probe = uiLabel(g, kind, 0, 0, 22, c.text, "semibold")
        kw = probe.boundingRect().width + 36
        g.removeChild(probe)
        uiPill(g, 0, 0, kw, 44, "0x12235FE6")
        uiLabel(g, kind, 18, 9, 22, c.text, "semibold")
        uiLabel(g, ch.group, kw + 18, 9, 24, c.muted2, "medium", 1200)
        uiLabel(g, ch.title, 0, 62, 60, c.text, "display", 1752)
        posSec = m.video.position
        dur = m.video.duration
        icon = "pause"
        if m.paused then icon = "play"
        uiIcon(g, icon, 0, 164, 42, c.white)
        pl = uiLabel(g, clockText(posSec), 66, 170, 24, c.muted2)
        dtxt = "--:--"
        if dur > 0 then dtxt = clockText(dur)
        dl = uiLabel(g, dtxt, 0, 170, 24, c.muted2, "regular", 1752, 1, "right")
        bx = 66 + 120
        bw = 1752 - bx - 130
        uiPill(g, bx, 182, bw, 9, c.track)
        if dur > 0 then
            frac = posSec / dur
            if frac > 1 then frac = 1
            uiPill(g, bx, 182, bw * frac + 9, 9, c.menta)
        end if
        hints = g.createChild("Group")
        hints.translation = [0, 236]
        ptxt = "Pausa"
        if m.paused then ptxt = "Seguir"
        hx = keycap(hints, 0, 0, "OK", "", ptxt)
        hx = keycap(hints, hx, 0, "◀ ▶", "", "10 segundos")
        hx = keycap(hints, hx, 0, "Atrás", "", "Salir y guardar dónde vas")
    end if
end sub

function clockText(sec as dynamic) as string
    s = Int(sec)
    h = Int(s / 3600)
    mi = Int((s mod 3600) / 60)
    ss = s mod 60
    if h > 0 then return h.ToStr() + ":" + pad2(mi) + ":" + pad2(ss)
    return mi.ToStr() + ":" + pad2(ss)
end function

sub onPosition()
    if m.isVod and m.banner.visible then drawBanner()
end sub

sub savePosition()
    if m.ch = invalid or not m.isVod then return
    posSec = Int(m.video.position)
    dur = Int(m.video.duration)
    if posSec < 5 then return
    if dur > 0 and posSec > dur * 0.95 then
        app("forgetRecent", {id: m.ch.id})
    else
        app("recordRecent", {id: m.ch.id, kind: m.ch.kind, posSec: posSec, dur: dur})
    end if
end sub

' ------------------------------------------------------------------ cambiar de canal
sub zapBy(delta as integer)
    if m.zap = invalid then return
    count = m.zap.getChildCount()
    if count < 2 then return
    curId = m.ch.id
    if m.pendingId <> invalid then curId = m.pendingId
    idx = 0
    for i = 0 to count - 1
        if m.zap.getChild(i).id = curId then
            idx = i
            exit for
        end if
    end for
    idx = (idx + delta + count) mod count
    m.pendingId = m.zap.getChild(idx).id
    sfx("tick")
    uiClear(m.errorG)
    showBanner()
    m.zapTimer.control = "stop"
    m.zapTimer.control = "start"
end sub

sub onZapTimer()
    id = m.pendingId
    if id = invalid then return
    m.pendingId = invalid
    m.video.control = "stop"
    app("recordRecent", {id: id, kind: "LIVE", posSec: 0, dur: 0})
    startChannel(id)
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "back" then
        m.zapTimer.control = "stop"
        savePosition()
        m.saveTimer.control = "stop"
        m.video.control = "stop"
        return false
    end if
    if key = "options" then
        if m.ch <> invalid then
            on = app("toggleFav", {id: m.ch.id})
            if on then
                sfx("on")
                app("toast", {title: "Agregado a favoritos", text: m.ch.title})
            else
                sfx("off")
                app("toast", {title: "Quitado de favoritos", text: m.ch.title})
            end if
        end if
        return true
    end if
    if not m.isVod then
        if key = "up" then
            zapBy(1)
        else if key = "down" then
            zapBy(-1)
        else if key = "OK" then
            if m.errorG.getChildCount() > 0 then
                sfx("ok")
                retry()
            else if m.banner.visible then
                m.bannerTimer.control = "stop"
                m.banner.visible = false
                m.shade.visible = false
            else
                showBanner()
            end if
        else
            showBanner()
        end if
        return true
    end if
    ' películas y series
    if key = "OK" or key = "play" then
        if m.errorG.getChildCount() > 0 then
            retry()
        else if m.video.state = "paused" then
            m.video.control = "resume"
            m.paused = false
            showBanner()
        else
            m.video.control = "pause"
        end if
        return true
    else if key = "left" or key = "rev" or key = "replay" then
        seekBy(-10)
        return true
    else if key = "right" or key = "fwd" then
        seekBy(10)
        return true
    end if
    showBanner()
    return true
end function

sub seekBy(sec as integer)
    p = m.video.position + sec
    if p < 0 then p = 0
    if m.video.duration > 0 and p > m.video.duration - 3 then p = m.video.duration - 3
    m.video.seek = p
    sfx("tick")
    showBanner()
end sub

function onKeyboardText(p as object) as dynamic
    return true
end function
