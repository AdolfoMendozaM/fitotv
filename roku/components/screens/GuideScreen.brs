' Guía: qué pasa ahora y en las próximas dos horas, canal por canal.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.body = m.top.createChild("Group")
    m.rail = m.top.createChild("Rail")
    m.rail.current = "guia"
    m.inRail = false
    m.previewTimer = m.top.createChild("Timer")
    m.previewTimer.duration = 1.0
    m.previewTimer.observeField("fire", "startPreview")
    m.cache = {}
    m.asked = {}
    m.reqN = 0
    m.zone = "rows"
    m.row = 0
    m.top0 = 0
    m.col = 0
    m.global.epg.observeFieldScoped("result", "onEpg")
    m.global.epg.observeFieldScoped("state", "onEpgState")
    ' medidas
    m.X = 168
    m.CH = 435
    m.TL = 615
    m.TW = 1257
    m.Y0 = 420
    m.RH = 96
    m.STEP = 108
    m.VIS = 6
end sub

sub onActiveChange()
    if m.top.active then
        st = app("getState")
        if st.profile <> invalid then m.rail.avatar = st.profile.avatar
        build()
    else
        stopPreview()
    end if
end sub

sub build()
    uiClear(m.body)
    c = m.c
    m.groups = app("liveGroups", {withFavs: true})
    if m.global.content = invalid or m.groups.Count() = 0 then
        m.empty = true
        g = m.body.createChild("Group")
        g.translation = [600, 340]
        uiCircle(g, 312, 0, 96, c.s2)
        uiIcon(g, "guide", 336, 24, 48, c.num)
        uiLabel(g, "La guía está vacía", 0, 130, 51, c.text, "display", 720, 1, "center")
        uiLabel(g, "Agrega una lista para ver qué están pasando.", 0, 204, 28, c.muted, "regular", 720, 2, "center")
        m.emptyBtn = uiButton(g, 0, 290, "Agregar una lista", "plus", "primary")
        m.emptyBtn.node.translation = [(720 - m.emptyBtn.w) / 2, 290]
        uiButtonFocus(m.emptyBtn, not m.inRail)
        m.top.setFocus(true)
        return
    end if
    m.empty = false
    want = app("getGroup")
    m.gi = 0
    found = false
    for i = 0 to m.groups.Count() - 1
        if m.groups[i].name = want then
            m.gi = i
            found = true
        end if
    end for
    if not found and m.groups.Count() > 1 and m.groups[0].name = "__fav__" then m.gi = 1
    m.chans = m.groups[m.gi].node
    t = nowSec()
    m.winStart = t - (t mod 1800)
    m.winEnd = m.winStart + 7200
    if m.row >= m.chans.getChildCount() then
        m.row = 0
        m.top0 = 0
    end if
    if m.top0 > m.row then m.top0 = m.row
    m.focusT = t

    ' arriba: lo enfocado y la vista previa
    m.info = m.body.createChild("Group")
    m.info.translation = [m.X, 36]
    pv = m.body.createChild("Group")
    pv.translation = [1392, 36]
    uiBox(pv, 0, 0, 480, 270, c.s3, 26)
    m.pvHolder = pv.createChild("Group")
    m.video = pv.createChild("Video")
    m.video.translation = [6, 6]
    m.video.width = 468
    m.video.height = 258
    m.video.enableUI = false
    m.video.visible = false
    m.video.observeField("state", "onPreviewState")
    uiPill(pv, 18, 18, 168, 42, "0x0A1747D9")
    uiLabel(pv, "Vista previa", 18, 26, 20, c.text, "semibold", 168, 1, "center")

    ' encabezado: grupo y horas
    hd = m.body.createChild("Group")
    hd.translation = [m.X, 336]
    m.groupRing = uiRing(hd, 0, 0, m.CH, 60, 0, true)
    m.groupBg = uiPill(hd, 0, 0, m.CH, 60, c.s2)
    gl = uiLabel(hd, "Grupo", 24, 16, 22, c.muted)
    gx = 24 + gl.boundingRect().width + 12
    uiLabel(hd, m.groups[m.gi].label, gx, 16, 22, c.text, "bold", m.CH - gx - 60)
    uiIcon(hd, "chevron_down", m.CH - 48, 17, 26, c.muted)
    for k = 0 to 3
        tt = m.winStart + k * 1800
        txt = hhmm(tt)
        if k = 0 then txt = "Hoy " + txt
        uiLabel(m.body, txt, xOf(tt) + 6, 352, 22, c.muted)
    end for
    m.rowsG = m.body.createChild("Group")
    drawRows()
    focusChanged()
end sub

function xOf(t as integer) as float
    f = (t - m.winStart) / (m.winEnd - m.winStart)
    if f < 0 then f = 0
    if f > 1 then f = 1
    return m.TL + m.TW * f
end function

function keyOf(n as object) as string
    cfg = m.global.epg.config
    if cfg <> invalid and cfg.mode = "xtream" and n.sid <> invalid and n.sid <> "" then return n.sid
    if n.tvgId <> invalid then return LCase(n.tvgId)
    return ""
end function

function progsOf(n as object) as object
    k = keyOf(n)
    out = []
    if k = "" or not m.cache.DoesExist(k) then return out
    for each p in m.cache[k]
        if p.e > m.winStart and p.s < m.winEnd then out.Push(p)
    end for
    return out
end function

' ------------------------------------------------------------------ filas
sub drawRows()
    c = m.c
    uiClear(m.rowsG)
    m.rowNodes = []
    total = m.chans.getChildCount()
    need = []
    t = nowSec()
    last = m.top0 + m.VIS - 1
    if last > total - 1 then last = total - 1
    for r = m.top0 to last
        n = m.chans.getChild(r)
        y = m.Y0 + (r - m.top0) * m.STEP
        g = m.rowsG.createChild("Group")
        g.translation = [0, y]
        cell = uiBox(g, m.X, 0, m.CH, m.RH, c.s1, 21)
        if n.num <> invalid and n.num > 0 then uiLabel(g, n.num.ToStr(), m.X + 18, 26, 33, c.num, "display", 66)
        uiLogoPlate(g, n.title, n.HDPosterUrl, m.X + 90, 21, 90, 54, 12)
        uiLabel(g, n.title, m.X + 198, 18, 21, c.text, "semibold", m.CH - 214, 2)
        nl = g.createChild("Rectangle")
        nl.color = c.menta
        nl.width = 3
        nl.height = m.RH
        nl.translation = [xOf(t), 0]
        progs = progsOf(n)
        k = keyOf(n)
        if k <> "" and not m.asked.DoesExist(k) then need.Push(k)
        blocks = []
        if progs.Count() = 0 then
            b = block(g, m.TL, m.TW - 9, "Sin información de la guía", "Presiona OK para ver el canal", "live")
            b.p = invalid
            blocks.Push(b)
        else
            for each p in progs
                l = xOf(maxOf(p.s, m.winStart))
                rr = xOf(minOf(p.e, m.winEnd))
                w = rr - l - 9
                if w < 36 then w = 36
                kind = "next"
                if p.e <= t then kind = "past"
                if p.s <= t and p.e > t then kind = "live"
                b = block(g, l, w, p.t, hhmm(p.s) + " a " + hhmm(p.e), kind)
                b.p = p
                blocks.Push(b)
            end for
        end if
        m.rowNodes.Push({cell: cell, blocks: blocks, node: n})
    end for
    if need.Count() > 0 then
        for each k in need
            m.asked[k] = true
        end for
        m.reqN = m.reqN + 1
        m.global.epg.request = {reqId: "guide" + m.reqN.ToStr(), keys: need}
    end if
    paintFocus()
end sub

function maxOf(a as dynamic, b as dynamic) as dynamic
    if a > b then return a
    return b
end function

function minOf(a as dynamic, b as dynamic) as dynamic
    if a < b then return a
    return b
end function

function block(parent as object, x as float, w as float, title as string, time as string, kind as string) as object
    c = m.c
    g = parent.createChild("Group")
    g.translation = [x, 0]
    ring = uiRing(g, 0, 0, w, m.RH, 21)
    col = c.s2
    if kind = "past" then col = c.noche
    if kind = "live" then col = c.s3
    out = invalid
    if kind = "past" then
        out = g.createChild("Poster")
        out.uri = radiusFile(21, true)
        out.blendColor = c.track
        out.width = w
        out.height = m.RH
    end if
    bg = uiBox(g, 0, 0, w, m.RH, col, 21)
    tl = invalid
    sl = invalid
    if w > 72 then
        tc = c.text
        if kind = "past" then tc = c.dim
        tl = uiLabel(g, title, 21, 16, 22, tc, "semibold", w - 42)
        sl = uiLabel(g, time, 21, 52, 19, c.muted, "regular", w - 42)
    end if
    return {ring: ring, bg: bg, base: col, title: tl, time: sl, kind: kind}
end function

sub paintFocus()
    c = m.c
    for i = 0 to m.rowNodes.Count() - 1
        rn = m.rowNodes[i]
        r = m.top0 + i
        hl = r = m.row
        if hl then rn.cell.blendColor = c.s3 else rn.cell.blendColor = c.s1
        for j = 0 to rn.blocks.Count() - 1
            b = rn.blocks[j]
            f = not m.inRail and m.zone = "rows" and hl and j = m.col
            b.ring.visible = f
            if f then b.bg.blendColor = c.azul else b.bg.blendColor = b.base
            if b.title <> invalid then
                if f then
                    b.title.color = c.white
                    b.time.color = "0xDDE4FFFF"
                else
                    if b.kind = "past" then b.title.color = c.dim else b.title.color = c.text
                    b.time.color = c.muted
                end if
            end if
        end for
    end for
    gf = not m.inRail and m.zone = "group"
    m.groupRing.visible = gf
    if gf then m.groupBg.blendColor = c.s3 else m.groupBg.blendColor = c.s2
end sub

' Elige el programa de la fila que cae en la hora enfocada.
sub pickCol()
    rn = m.rowNodes[m.row - m.top0]
    m.col = 0
    for j = 0 to rn.blocks.Count() - 1
        p = rn.blocks[j].p
        if p <> invalid and p.s <= m.focusT and p.e > m.focusT then m.col = j
    end for
    if rn.blocks.Count() > 0 then
        p = rn.blocks[m.col].p
        if p <> invalid and (p.s > m.focusT or p.e <= m.focusT) then
            ' sin coincidencia exacta: el más cercano
            best = 0
            bd = 999999
            for j = 0 to rn.blocks.Count() - 1
                q = rn.blocks[j].p
                if q <> invalid then
                    d = Abs(q.s - m.focusT)
                    if d < bd then
                        bd = d
                        best = j
                    end if
                end if
            end for
            m.col = best
        end if
    end if
end sub

' ------------------------------------------------------------------ arriba
sub focusChanged()
    c = m.c
    uiClear(m.info)
    if m.rowNodes.Count() = 0 then return
    rn = m.rowNodes[m.row - m.top0]
    n = rn.node
    x = 0
    if n.num <> invalid and n.num > 0 then
        nl = uiLabel(m.info, n.num.ToStr(), 0, 0, 45, c.num, "display")
        x = nl.boundingRect().width + 18
    end if
    uiLogoPlate(m.info, n.title, n.HDPosterUrl, x, 4, 96, 57, 14)
    uiLabel(m.info, n.title, x + 114, 16, 27, c.text, "semibold", 1080 - x - 114)
    p = invalid
    if rn.blocks.Count() > 0 then p = rn.blocks[m.col].p
    W = 1180
    if p = invalid then
        uiLabel(m.info, "Sin información de la guía", 0, 84, 48, c.text, "display", W)
        uiLabel(m.info, "Este canal no trae guía en tu lista. Presiona OK para verlo.", 0, 152, 23, c.muted, "regular", W)
    else
        uiLabel(m.info, p.t, 0, 84, 48, c.text, "display", W)
        t = nowSec()
        tl = uiLabel(m.info, hhmm(p.s) + " a " + hhmm(p.e), 0, 152, 23, c.text)
        bx = tl.boundingRect().width + 18
        if p.s <= t and p.e > t then
            frac = (t - p.s) / (p.e - p.s)
            uiPill(m.info, bx, 162, 180, 8, c.track)
            uiPill(m.info, bx, 162, 180 * frac + 8, 8, c.menta)
            uiLabel(m.info, "empezó hace " + Int((t - p.s) / 60).ToStr() + " min", bx + 198, 152, 23, c.muted)
        else if p.s > t then
            uiLabel(m.info, "empieza en " + Int((p.s - t) / 60).ToStr() + " min", bx, 152, 23, c.muted)
        end if
        if p.d <> invalid and p.d <> "" then uiLabel(m.info, p.d, 0, 194, 23, c.muted2, "regular", 900, 2)
    end if
    if m.pvId = invalid or m.pvId <> n.id then schedulePreview(n)
end sub

sub schedulePreview(n as object)
    stopPreview()
    m.pvId = n.id
    uiClear(m.pvHolder)
    uiLogoPlate(m.pvHolder, n.title, n.HDPosterUrl, 165, 90, 150, 88, 16)
    if m.top.active then m.previewTimer.control = "start"
end sub

sub startPreview()
    if m.pvId = invalid or not m.top.active then return
    n = app("channelNode", {id: m.pvId})
    if n = invalid then return
    v = CreateObject("roSGNode", "ContentNode")
    v.url = n.url
    v.streamFormat = n.streamFormat
    v.live = true
    if n.ua <> invalid and n.ua <> "" then v.httpHeaders = ["User-Agent:" + n.ua]
    m.video.content = v
    m.video.control = "play"
end sub

sub stopPreview()
    m.previewTimer.control = "stop"
    if m.video <> invalid then
        m.video.control = "stop"
        m.video.visible = false
    end if
    m.pvId = invalid
end sub

sub onPreviewState()
    if m.video.state = "playing" then m.video.visible = true
    if m.video.state = "error" then m.video.visible = false
end sub

' ------------------------------------------------------------------ guía que llega
sub onEpg(ev as object)
    r = ev.getData()
    if r = invalid or r.reqId = invalid or Left(r.reqId, 5) <> "guide" then return
    changed = false
    for each k in r.data
        m.cache[k] = r.data[k]
        changed = true
    end for
    if changed and not m.empty and m.top.active then
        drawRows()
        if m.zone = "rows" then pickCol()
        paintFocus()
        focusChanged()
    end if
end sub

sub onEpgState()
    m.cache = {}
    m.asked = {}
    if m.top.active and not m.empty then drawRows()
end sub

' ------------------------------------------------------------------ teclas
sub moveRow(d as integer)
    total = m.chans.getChildCount()
    nr = m.row + d
    if nr < 0 then
        m.zone = "group"
        sfx("tick")
        paintFocus()
        return
    end if
    if nr > total - 1 then return
    ' hora de referencia: la del programa enfocado
    rn = m.rowNodes[m.row - m.top0]
    if rn.blocks.Count() > 0 and rn.blocks[m.col].p <> invalid then
        p = rn.blocks[m.col].p
        m.focusT = maxOf(p.s, nowSec())
        if m.focusT >= p.e then m.focusT = p.s
    end if
    m.row = nr
    sfx("tick")
    if m.row < m.top0 then
        m.top0 = m.row
        drawRows()
    else if m.row > m.top0 + m.VIS - 1 then
        m.top0 = m.row - m.VIS + 1
        drawRows()
    end if
    pickCol()
    paintFocus()
    focusChanged()
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.inRail then return railKey(key)
    if m.empty then
        if key = "left" then
            railEnter()
            uiButtonFocus(m.emptyBtn, false)
            return true
        end if
        if key = "OK" then
            sfx("ok")
            app("go", {screen: "AddListScreen", params: {firstTime: false}})
            return true
        end if
        return key <> "back"
    end if
    if key = "back" and m.zone <> "rows" then
        m.zone = "rows"
        paintFocus()
        return true
    end if
    if m.zone = "group" then
        if key = "down" then
            m.zone = "rows"
            sfx("tick")
            pickCol()
            paintFocus()
        else if key = "left" then
            railEnter()
            paintFocus()
        else if key = "OK" then
            sfx("ok")
            app("go", {screen: "GroupsScreen"})
        end if
        return true
    end if
    if key = "up" then
        moveRow(-1)
        return true
    else if key = "down" then
        moveRow(1)
        return true
    else if key = "left" then
        if m.col > 0 then
            m.col = m.col - 1
            sfx("tick")
            paintFocus()
            focusChanged()
        else
            railEnter()
            paintFocus()
        end if
        return true
    else if key = "right" then
        rn = m.rowNodes[m.row - m.top0]
        if m.col < rn.blocks.Count() - 1 then
            m.col = m.col + 1
            sfx("tick")
            paintFocus()
            focusChanged()
        end if
        return true
    else if key = "OK" or key = "play" then
        n = m.rowNodes[m.row - m.top0].node
        sfx("ok")
        stopPreview()
        app("play", {id: n.id, zap: m.chans})
        return true
    else if key = "options" then
        n = m.rowNodes[m.row - m.top0].node
        on = app("toggleFav", {id: n.id})
        if on then
            sfx("on")
            app("toast", {title: "Agregado a favoritos", text: n.title})
        else
            sfx("off")
            app("toast", {title: "Quitado de favoritos", text: n.title})
        end if
        return true
    end if
    return false
end function

sub railLeft()
    if m.empty then
        uiButtonFocus(m.emptyBtn, true)
    else
        paintFocus()
    end if
    m.top.setFocus(true)
end sub

function onKeyboardText(p as object) as dynamic
    return true
end function
