' Inicio: canal destacado con vista previa, continuar viendo, recientes, grupos y canales.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.body = m.top.createChild("Group")
    m.rail = m.top.createChild("Rail")
    m.rail.current = "inicio"
    m.inRail = false
    m.overlay = m.top.createChild("Group")
    m.scroll = m.top.createChild("Animation")
    m.scroll.duration = 0.25
    m.scroll.easeFunction = "outCubic"
    m.scrollI = m.scroll.createChild("Vector2DFieldInterpolator")
    m.scrollI.key = [0.0, 1.0]
    m.scrollI.fieldToInterp = "homeBody.translation"
    m.body.id = "homeBody"
    m.previewTimer = m.top.createChild("Timer")
    m.previewTimer.duration = 1.0
    m.previewTimer.observeField("fire", "startPreview")
    m.epgTimer = m.top.createChild("Timer")
    m.epgTimer.duration = 30
    m.epgTimer.repeat = true
    m.epgTimer.observeField("fire", "refreshHeroEpg")
    m.reqId = 0
    m.epgCache = {}
    m.built = false
    m.zone = "hero"
    m.heroIdx = 0
    m.chipIdx = 0
    m.contIdx = 0
    m.recIdx = 0
    m.heroCh = invalid
    m.global.epg.observeFieldScoped("result", "onEpg")
    m.global.epg.observeFieldScoped("state", "onEpgState")
end sub

sub onActiveChange()
    if m.top.active then
        scene().observeFieldScoped("listStatus", "onStatus")
        st = app("getState")
        if st.profile <> invalid then m.rail.avatar = st.profile.avatar
        stale = m.built and (m.global.content = invalid or m.builtContent = invalid or not m.global.content.isSameNode(m.builtContent))
        if not m.built or stale then
            onStatus()
        else
            m.groupName = app("getGroup")
            refreshRows()
            focusZone()
            schedulePreview()
        end if
        m.epgTimer.control = "start"
    else
        scene().unobserveFieldScoped("listStatus")
        stopPreview()
        m.epgTimer.control = "stop"
    end if
end sub

' ------------------------------------------------------------------ estado de la lista
sub onStatus()
    s = scene().listStatus
    if s = invalid or s.state = "refreshing" then return
    if m.built and s.state = "error" and m.global.content <> invalid then return
    if s.state = "loading" or (s.state = "checking" and m.global.content = invalid) then
        showMessage("loading", "Revisando tu lista…", "Las listas grandes tardan unos segundos.")
    else if s.state = "none" then
        showMessage("none", "Agrega una lista", "Con un enlace, un archivo o el usuario y contraseña de tu proveedor.")
    else if s.state = "error" and m.global.content = invalid then
        showMessage("error", "No pudimos abrir la lista.", s.message)
    else if m.global.content <> invalid then
        uiClear(m.overlay)
        m.msgButtons = invalid
        build()
        if s.stale = true then app("toast", {title: "Usamos la copia guardada", text: "La lista no respondió; te mostramos lo último que bajamos."})
    end if
end sub

sub showMessage(kind as string, title as string, text as string)
    stopPreview()
    uiClear(m.body)
    uiClear(m.overlay)
    m.built = false
    m.msgKind = kind
    c = m.c
    g = m.overlay.createChild("Group")
    g.translation = [600, 330]
    if kind = "loading" then
        uiSpinner(g, 300, 0, 96, c.menta)
    else
        uiCircle(g, 300, 0, 96, "0x12235FFF")
        icon = "plus"
        if kind = "error" then icon = "warn"
        uiIcon(g, icon, 324, 24, 48, c.ambar)
    end if
    uiLabel(g, title, 0, 130, 51, c.text, "display", 720, 1, "center")
    uiLabel(g, text, 0, 204, 28, c.muted, "regular", 720, 3, "center")
    m.msgButtons = []
    if kind = "none" then
        b = uiButton(g, 0, 330, "Agregar una lista", "plus", "primary")
        b.action = "add"
        m.msgButtons.Push(b)
    else if kind = "error" then
        b = uiButton(g, 0, 330, "Intentar otra vez", "refresh", "primary")
        b.action = "retry"
        m.msgButtons.Push(b)
        b2 = uiButton(g, b.w + 24, 330, "Cambiar de lista", "list", "secondary")
        b2.action = "lists"
        m.msgButtons.Push(b2)
    end if
    tw = 0
    for each b in m.msgButtons
        tw = tw + b.w + 24
    end for
    x = (720 - tw + 24) / 2
    for each b in m.msgButtons
        b.node.translation = [x, 330]
        x = x + b.w + 24
    end for
    m.msgIdx = 0
    paintMsg()
    m.top.setFocus(true)
end sub

sub paintMsg()
    if m.msgButtons = invalid then return
    for i = 0 to m.msgButtons.Count() - 1
        uiButtonFocus(m.msgButtons[i], not m.inRail and i = m.msgIdx)
    end for
end sub

' ------------------------------------------------------------------ construir
sub build()
    c = m.c
    content = m.global.content
    st = app("getState")
    m.state = st
    uiClear(m.body)
    m.builtContent = content
    m.body.translation = [0, 0]
    m.groupName = app("getGroup")
    loadGroups()

    ' pastilla de la lista
    l = st.activeList
    m.pill = m.body.createChild("Group")
    m.pill.translation = [168, 36]
    pname = l.name
    count = 0
    for each gg in m.groups
        if gg.name <> "__fav__" then count = count + gg.count
    end for
    ptxt = count.ToStr() + " canales"
    probe = uiLabel(m.pill, pname, 0, 0, 27, c.text, "semibold")
    pw = probe.boundingRect().width
    m.pill.removeChild(probe)
    probe = uiLabel(m.pill, ptxt, 0, 0, 23, c.muted)
    pw2 = probe.boundingRect().width
    m.pill.removeChild(probe)
    W = 90 + pw + 14 + pw2 + 56
    m.pillRing = uiRing(m.pill, 0, 0, W, 60, 0, true)
    m.pillBg = uiPill(m.pill, 0, 0, W, 60, "0x12235FFF")
    uiBox(m.pill, 12, 10, 40, 40, c.menta, 12)
    uiLabel(m.pill, UCase(Left(pname, 1)), 12, 15, 24, c.noche, "bold", 40, 1, "center")
    uiLabel(m.pill, pname, 66, 14, 27, c.text, "semibold")
    uiLabel(m.pill, ptxt, 66 + pw + 14, 17, 23, c.muted)
    uiIcon(m.pill, "chevron_down", W - 44, 16, 28, c.muted)

    ' héroe
    m.hero = m.body.createChild("Group")
    m.hero.translation = [168, 118]
    m.heroTop = m.hero.createChild("Group")
    m.heroTitle = uiLabel(m.hero, "", 0, 56, 66, c.text, "display", 1080)
    m.heroLine1 = uiLabel(m.hero, "", 0, 150, 27, c.muted, "regular", 1080)
    m.heroBar = m.hero.createChild("Group")
    m.heroLine2 = uiLabel(m.hero, "", 0, 192, 25, c.dim, "regular", 1080)
    m.heroBtns = []
    b = uiButton(m.hero, 0, 246, "Ver ahora", "play", "primary", 0, 72)
    b.action = "play"
    m.heroBtns.Push(b)
    b = uiButton(m.hero, b.w + 18, 246, "", "star", "secondary", 72, 72)
    b.action = "fav"
    m.heroBtns.Push(b)
    b2 = uiButton(m.hero, m.heroBtns[0].w + 18 + b.w + 18, 246, "", "list", "secondary", 72, 72)
    b2.action = "groups"
    m.heroBtns.Push(b2)

    ' vista previa
    pv = m.body.createChild("Group")
    pv.translation = [1290, 36]
    uiBox(pv, 0, 0, 570, 320, "0x0F1F5CFF", 24)
    m.pvHolder = pv.createChild("Group")
    m.video = pv.createChild("Video")
    m.video.translation = [8, 8]
    m.video.width = 554
    m.video.height = 304
    m.video.enableUI = false
    m.video.visible = false
    m.video.observeField("state", "onPreviewState")
    uiPill(pv, 18, 18, 180, 40, "0x0A1747CC")
    uiIcon(pv, "sound", 30, 26, 24, c.text)
    uiLabel(pv, "Vista previa", 62, 24, 21, c.text, "semibold")
    m.pvMsg = uiLabel(pv, "", 0, 250, 22, c.muted, "regular", 570, 1, "center")

    m.rows = m.body.createChild("Group")
    m.built = true
    refreshRows()
    m.zone = "hero"
    pickHero()
    focusZone()
end sub

sub loadGroups()
    m.groups = app("liveGroups", {withFavs: true})
    if m.groups.Count() = 0 then m.groups.Push({name: "", label: "Sin canales en vivo", count: 0, node: CreateObject("roSGNode", "ContentNode"), star: false})
    m.groupSel = 0
    found = false
    for i = 0 to m.groups.Count() - 1
        if m.groups[i].name = m.groupName then
            m.groupSel = i
            found = true
        end if
    end for
    ' sin grupo guardado, empieza en el primer grupo normal (no en Favoritos)
    if not found and m.groups.Count() > 1 and m.groups[0].name = "__fav__" then m.groupSel = 1
    m.groupName = m.groups[m.groupSel].name
end sub

sub refreshRows()
    if not m.built then return
    loadGroups()
    c = m.c
    uiClear(m.rows)
    st = app("getState")
    m.state = st
    m.cont = app("recents", {kind: "VOD"})
    m.recents = app("recents", {kind: "LIVE"})
    y = 450
    m.contY = -1
    m.recY = -1
    m.contNodes = []
    m.recNodes = []
    if m.cont.Count() > 0 then
        m.contY = y
        name = ""
        if st.profile <> invalid then name = st.profile.name
        uiLabel(m.rows, "Continuar viendo contenido de " + name, 168, y, 33, c.text, "display")
        x = 168
        for i = 0 to m.cont.Count() - 1
            if i < 4 then
                it = m.cont[i]
                g = m.rows.createChild("Group")
                g.translation = [x, y + 60]
                ring = uiRing(g, 0, 0, 400, 150, 18)
                bg = uiBox(g, 0, 0, 400, 150, "0x12235FFF", 18)
                uiBox(g, 12, 12, 84, 126, "0x0F1F5CFF", 10)
                if it.logo <> "" then uiImage(g, it.logo, 12, 12, 84, 126, "scaleToZoom")
                uiLabel(g, it.name, 114, 22, 25, c.text, "semibold", 270, 2)
                frac = 0
                if it.dur <> invalid and it.dur > 0 then frac = it.posSec / it.dur
                if frac > 1 then frac = 1
                uiPill(g, 114, 112, 260, 8, "0x1F3378FF")
                if frac > 0 then uiPill(g, 114, 112, 260 * frac + 8, 8, c.menta)
                m.contNodes.Push({ring: ring, bg: bg, item: it})
                x = x + 424
            end if
        end for
        y = y + 240
    end if
    if m.recents.Count() > 0 then
        m.recY = y
        uiLabel(m.rows, "Canales recientes", 168, y, 33, c.text, "display")
        uiLabel(m.rows, "Lo que está pasando ahora en los que visitaste", 480, y + 8, 24, c.muted)
        x = 168
        for i = 0 to m.recents.Count() - 1
            if i < 5 then
                it = m.recents[i]
                g = m.rows.createChild("Group")
                g.translation = [x, y + 60]
                ring = uiRing(g, 0, 0, 318, 120, 18)
                bg = uiBox(g, 0, 0, 318, 120, "0x12235FFF", 18)
                if it.num > 0 then uiLabel(g, it.num.ToStr(), 18, 16, 27, c.num, "display")
                uiLabel(g, it.name, 78, 18, 24, c.text, "semibold", 140)
                uiLogoPlate(g, it.name, it.logo, 226, 14, 78, 46, 10)
                uiCircle(g, 20, 78, 12, c.menta)
                uiLabel(g, "En vivo", 40, 72, 21, c.muted2, "medium", 260)
                m.recNodes.Push({ring: ring, bg: bg, item: it})
                x = x + 342
            end if
        end for
        y = y + 210
    end if
    ' grupos
    m.chipsY = y
    m.chips = []
    chipsG = m.rows.createChild("Group")
    chipsG.translation = [168, y]
    m.chipsG = chipsG
    x = 0
    for i = 0 to m.groups.Count()
        if i < m.groups.Count() then
            label = m.groups[i].label
            cnt = m.groups[i].count.ToStr()
        else
            label = "Todos los grupos"
            cnt = ""
        end if
        g = chipsG.createChild("Group")
        g.translation = [x, 0]
        probe = uiLabel(g, label, 0, 0, 24, c.text, "semibold")
        w1 = probe.boundingRect().width
        g.removeChild(probe)
        w = w1 + 52
        if cnt <> "" then w = w + 40
        if cnt = "" then w = w + 34
        ring = uiRing(g, 0, 0, w, 60, 0, true)
        bg = uiPill(g, 0, 0, w, 60, "0x12235FFF")
        lx = 26
        if i < m.groups.Count() and m.groups[i].star then
            uiIcon(g, "star_fill", 22, 16, 26, c.ambar)
            lx = 58
            w = w + 32
            bg.width = w
            ring.width = w + 12
        end if
        lb = uiLabel(g, label, lx, 15, 24, c.muted2, "semibold")
        cl = invalid
        if cnt <> "" then
            cl = uiLabel(g, cnt, lx + w1 + 12, 18, 20, c.dim, "medium")
        else
            uiIcon(g, "grid", 26 + w1 + 8, 16, 26, c.muted2)
        end if
        m.chips.Push({node: g, ring: ring, bg: bg, label: lb, count: cl, x: x, w: w})
        x = x + w + 14
    end for
    m.chipIdx = m.groupSel
    y = y + 100
    m.gridTitleY = y
    grp = m.groups[m.groupSel]
    m.gridTitle = uiLabel(m.rows, grp.label, 168, y, 36, c.text, "display")
    tw = m.gridTitle.boundingRect().width
    m.gridCount = uiLabel(m.rows, grp.count.ToStr() + " canales", 168 + tw + 18, y + 10, 24, c.muted)
    if m.grid = invalid then
        m.grid = CreateObject("roSGNode", "MarkupGrid")
        m.grid.itemComponentName = "ChannelCard"
        m.grid.numColumns = 5
        m.grid.numRows = 4
        m.grid.itemSize = [330, 198]
        m.grid.itemSpacing = [12, 12]
        m.grid.drawFocusFeedback = false
        m.grid.vertFocusAnimationStyle = "floatingFocus"
        m.grid.observeField("itemFocused", "onGridFocus")
        m.grid.observeField("itemSelected", "onGridSelect")
    end if
    m.rows.appendChild(m.grid)
    m.grid.translation = [162, y + 64]
    if m.grid.content = invalid or not m.grid.content.isSameNode(grp.node) then
        m.grid.content = grp.node
        m.grid.jumpToItem = 0
    end if
    paintChips()
end sub

' ------------------------------------------------------------------ héroe y vista previa
sub pickHero()
    if m.zone = "cont" and m.cont.Count() > 0 then
        setHero(m.cont[m.contIdx])
    else if m.zone = "recent" and m.recents.Count() > 0 then
        setHero(m.recents[m.recIdx])
    else if m.zone = "grid" then
        n = m.grid.content.getChild(m.grid.itemFocused)
        if n <> invalid then setHero(nodeAA(n))
    else if m.heroCh = invalid then
        if m.recents.Count() > 0 then
            setHero(m.recents[0])
        else
            n = m.groups[m.groupSel].node.getChild(0)
            if n <> invalid then setHero(nodeAA(n))
        end if
    end if
end sub

function nodeAA(n as object) as object
    return {id: n.id, name: n.title, url: n.url, logo: n.HDPosterUrl, num: n.num, group: n.group, kind: n.kind, tvgId: n.tvgId, sid: n.sid}
end function

sub setHero(ch as object)
    if m.heroCh <> invalid and m.heroCh.id = ch.id then return
    m.heroCh = ch
    c = m.c
    uiClear(m.heroTop)
    x = 0
    if ch.num <> invalid and ch.num > 0 then
        nl = uiLabel(m.heroTop, ch.num.ToStr(), 0, 0, 36, c.num, "display")
        x = nl.boundingRect().width + 18
    end if
    uiLogoPlate(m.heroTop, ch.name, ch.logo, x, -2, 78, 46, 10)
    uiLabel(m.heroTop, ch.name, x + 96, 6, 29, c.text, "semibold", 800)
    if ch.kind = "LIVE" then
        m.heroTitle.text = "En vivo"
        m.heroLine1.text = "Sin información de la guía para este canal"
    else
        m.heroTitle.text = ch.name
        if ch.kind = "MOVIE" then m.heroLine1.text = "Película" else m.heroLine1.text = "Serie"
    end if
    m.heroLine2.text = ""
    uiClear(m.heroBar)
    if app("isFav", {id: ch.id}) then m.heroBtns[1].icon.uri = "pkg:/images/icons/star_fill.png" else m.heroBtns[1].icon.uri = "pkg:/images/icons/star.png"
    refreshHeroEpg()
    schedulePreview()
end sub

function epgKey(ch as object) as string
    if ch.sid <> invalid and ch.sid <> "" then
        cfg = m.global.epg.config
        if cfg <> invalid and cfg.mode = "xtream" then return ch.sid
    end if
    if ch.tvgId <> invalid then return LCase(ch.tvgId)
    return ""
end function

sub refreshHeroEpg()
    if m.heroCh = invalid or m.heroCh.kind <> "LIVE" then return
    k = epgKey(m.heroCh)
    if k = "" then return
    if m.epgCache.DoesExist(k) then
        showHeroEpg(m.epgCache[k])
        return
    end if
    m.reqId = m.reqId + 1
    m.global.epg.request = {reqId: "home" + m.reqId.ToStr(), keys: [k]}
end sub

sub onEpgState()
    m.epgCache = {}
    refreshHeroEpg()
end sub

sub onEpg(ev as object)
    r = ev.getData()
    if r = invalid or r.reqId = invalid or Left(r.reqId, 4) <> "home" then return
    for each k in r.data
        m.epgCache[k] = r.data[k]
    end for
    if m.heroCh <> invalid then
        k = epgKey(m.heroCh)
        if m.epgCache.DoesExist(k) then showHeroEpg(m.epgCache[k])
    end if
end sub

sub showHeroEpg(progs as object)
    now = nowSec()
    cur = invalid
    nxt = invalid
    for each p in progs
        if p.s <= now and p.e > now then
            cur = p
        else if p.s > now and nxt = invalid then
            nxt = p
        end if
    end for
    uiClear(m.heroBar)
    if cur <> invalid then
        m.heroTitle.text = cur.t
        m.heroLine1.text = "Ahora, " + hhmm(cur.s) + " a " + hhmm(cur.e)
        frac = (now - cur.s) / (cur.e - cur.s)
        uiPill(m.heroBar, 330, 162, 220, 8, "0x1F3378FF")
        uiPill(m.heroBar, 330, 162, 220 * frac + 8, 8, m.c.menta)
        mins = Int((cur.e - now) / 60)
        uiLabel(m.heroBar, "quedan " + mins.ToStr() + " min", 570, 152, 23, m.c.muted)
    end if
    if nxt <> invalid then m.heroLine2.text = "Después, " + nxt.t + " a las " + hhmm(nxt.s)
end sub

sub schedulePreview()
    stopPreview()
    if m.heroCh = invalid or not m.top.active then return
    uiClear(m.pvHolder)
    uiLogoPlate(m.pvHolder, m.heroCh.name, m.heroCh.logo, 205, 90, 160, 94, 16)
    m.pvMsg.text = "Cargando vista previa…"
    m.previewTimer.control = "start"
end sub

sub startPreview()
    if m.heroCh = invalid or not m.top.active then return
    n = app("channelNode", {id: m.heroCh.id})
    if n = invalid then return
    v = CreateObject("roSGNode", "ContentNode")
    v.url = n.url
    v.streamFormat = n.streamFormat
    v.title = n.title
    if n.ua <> invalid and n.ua <> "" then v.httpHeaders = ["User-Agent:" + n.ua]
    m.video.content = v
    m.video.control = "play"
end sub

sub stopPreview()
    if m.previewTimer <> invalid then m.previewTimer.control = "stop"
    if m.video <> invalid then
        m.video.control = "stop"
        m.video.visible = false
    end if
end sub

sub onPreviewState()
    s = m.video.state
    if s = "playing" then
        m.video.visible = true
        m.pvMsg.text = ""
    else if s = "error" then
        m.video.visible = false
        m.pvMsg.text = "Sin vista previa por ahora"
    end if
end sub

' ------------------------------------------------------------------ foco
function zoneOrder() as object
    z = ["pill", "hero"]
    if m.cont.Count() > 0 then z.Push("cont")
    if m.recents.Count() > 0 then z.Push("recent")
    z.Push("chips")
    z.Push("grid")
    return z
end function

sub moveZone(delta as integer)
    z = zoneOrder()
    idx = 0
    for i = 0 to z.Count() - 1
        if z[i] = m.zone then idx = i
    end for
    idx = idx + delta
    if idx < 0 or idx >= z.Count() then return
    m.zone = z[idx]
    sfx("tick")
    focusZone()
    pickHero()
end sub

sub focusZone()
    if not m.built then return
    target = 0
    if m.zone = "cont" then
        target = -(m.contY - 420)
    else if m.zone = "recent" then
        target = -(m.recY - 420)
    else if m.zone = "chips" then
        target = -(m.chipsY - 560)
    else if m.zone = "grid" then
        target = -(m.gridTitleY - 48)
    end if
    if target > 0 then target = 0
    m.scrollI.keyValue = [m.body.translation, [0, target]]
    m.scroll.control = "start"
    paintAll()
    if m.zone = "grid" and not m.inRail then
        m.grid.setFocus(true)
    else
        m.top.setFocus(true)
    end if
end sub

sub paintAll()
    nr = not m.inRail
    m.pillRing.visible = nr and m.zone = "pill"
    for i = 0 to m.heroBtns.Count() - 1
        uiButtonFocus(m.heroBtns[i], nr and m.zone = "hero" and i = m.heroIdx)
    end for
    for i = 0 to m.contNodes.Count() - 1
        f = nr and m.zone = "cont" and i = m.contIdx
        m.contNodes[i].ring.visible = f
        if f then m.contNodes[i].bg.blendColor = "0x1A2E78FF" else m.contNodes[i].bg.blendColor = "0x12235FFF"
    end for
    for i = 0 to m.recNodes.Count() - 1
        f = nr and m.zone = "recent" and i = m.recIdx
        m.recNodes[i].ring.visible = f
        if f then m.recNodes[i].bg.blendColor = "0x1A2E78FF" else m.recNodes[i].bg.blendColor = "0x12235FFF"
    end for
    paintChips()
end sub

sub paintChips()
    if m.chips = invalid then return
    nr = not m.inRail
    off = 0
    if m.chips.Count() > 0 then
        ch = m.chips[m.chipIdx]
        if ch.x + ch.w > 1640 then off = -(ch.x + ch.w - 1640)
    end if
    m.chipsG.translation = [168 + off, m.chipsY]
    for i = 0 to m.chips.Count() - 1
        ch = m.chips[i]
        sel = i = m.groupSel
        f = nr and m.zone = "chips" and i = m.chipIdx
        ch.ring.visible = f
        col = "0x12235FFF"
        if sel then col = "0x2350E0FF"
        if f and not sel then col = "0x1A2E78FF"
        ch.bg.blendColor = col
        if sel or f then ch.label.color = "0xFFFFFFFF" else ch.label.color = "0xC8D2F5FF"
        if ch.count <> invalid then
            if sel then ch.count.color = "0xDDE4FFFF" else ch.count.color = "0x8E9BD0FF"
        end if
    end for
end sub

sub onGridFocus()
    if m.zone <> "grid" then return
    n = m.grid.content.getChild(m.grid.itemFocused)
    if n <> invalid then setHero(nodeAA(n))
end sub

sub onGridSelect()
    n = m.grid.content.getChild(m.grid.itemSelected)
    if n = invalid then return
    sfx("ok")
    stopPreview()
    app("play", {id: n.id, zap: m.grid.content})
end sub

sub selectGroup(i as integer)
    m.groupSel = i
    grp = m.groups[i]
    m.groupName = grp.name
    app("setGroup", {name: grp.name})
    m.grid.content = grp.node
    m.grid.jumpToItem = 0
    m.gridTitle.text = grp.label
    tw = m.gridTitle.boundingRect().width
    m.gridCount.text = grp.count.ToStr() + " canales"
    m.gridCount.translation = [168 + tw + 18, m.gridTitleY + 10]
    paintChips()
end sub

sub heroAction()
    a = m.heroBtns[m.heroIdx].action
    if a = "play" and m.heroCh <> invalid then
        sfx("ok")
        stopPreview()
        app("play", {id: m.heroCh.id})
    else if a = "fav" and m.heroCh <> invalid then
        on = app("toggleFav", {id: m.heroCh.id})
        if on then
            sfx("on")
            m.heroBtns[1].icon.uri = "pkg:/images/icons/star_fill.png"
            app("toast", {title: "Agregado a favoritos", text: m.heroCh.name})
        else
            sfx("off")
            m.heroBtns[1].icon.uri = "pkg:/images/icons/star.png"
        end if
    else if a = "groups" then
        sfx("ok")
        app("go", {screen: "GroupsScreen"})
    end if
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.inRail then return railKey(key)
    if not m.built then
        if key = "left" then
            railEnter()
            paintMsg()
            return true
        end if
        if m.msgButtons <> invalid and m.msgButtons.Count() > 0 then
            if key = "right" and m.msgIdx < m.msgButtons.Count() - 1 then m.msgIdx = m.msgIdx + 1
            if key = "OK" then
                a = m.msgButtons[m.msgIdx].action
                sfx("ok")
                if a = "add" then app("go", {screen: "AddListScreen", params: {firstTime: false}})
                if a = "retry" then app("refreshActive")
                if a = "lists" then app("go", {screen: "ListsScreen"})
            end if
            paintMsg()
        end if
        return key <> "back"
    end if
    if key = "back" then
        if m.zone <> "hero" then
            m.zone = "hero"
            focusZone()
            return true
        end if
        return false
    end if
    if key = "options" and m.heroCh <> invalid then
        m.heroIdx = 1
        heroAction()
        return true
    end if
    if key = "up" then
        moveZone(-1)
        return true
    else if key = "down" then
        moveZone(1)
        return true
    else if key = "left" then
        moved = false
        if m.zone = "hero" and m.heroIdx > 0 then
            m.heroIdx = m.heroIdx - 1
            moved = true
        else if m.zone = "cont" and m.contIdx > 0 then
            m.contIdx = m.contIdx - 1
            moved = true
        else if m.zone = "recent" and m.recIdx > 0 then
            m.recIdx = m.recIdx - 1
            moved = true
        else if m.zone = "chips" and m.chipIdx > 0 then
            m.chipIdx = m.chipIdx - 1
            moved = true
        end if
        if moved then
            sfx("tick")
            pickHero()
            paintAll()
        else
            railEnter()
            paintAll()
            m.top.setFocus(true)
        end if
        return true
    else if key = "right" then
        if m.zone = "hero" and m.heroIdx < m.heroBtns.Count() - 1 then m.heroIdx = m.heroIdx + 1
        if m.zone = "cont" and m.contIdx < m.contNodes.Count() - 1 then m.contIdx = m.contIdx + 1
        if m.zone = "recent" and m.recIdx < m.recNodes.Count() - 1 then m.recIdx = m.recIdx + 1
        if m.zone = "chips" and m.chipIdx < m.chips.Count() - 1 then m.chipIdx = m.chipIdx + 1
        sfx("tick")
        pickHero()
        paintAll()
        return true
    else if key = "OK" then
        if m.zone = "pill" then
            sfx("ok")
            app("go", {screen: "ListsScreen"})
        else if m.zone = "hero" then
            heroAction()
        else if m.zone = "cont" then
            sfx("ok")
            stopPreview()
            app("play", {id: m.cont[m.contIdx].id})
        else if m.zone = "recent" then
            sfx("ok")
            stopPreview()
            app("play", {id: m.recents[m.recIdx].id})
        else if m.zone = "chips" then
            sfx("ok")
            if m.chipIdx >= m.groups.Count() then
                app("go", {screen: "GroupsScreen"})
            else
                selectGroup(m.chipIdx)
                m.zone = "grid"
                focusZone()
            end if
        end if
        return true
    end if
    return false
end function

sub railLeft()
    if m.built then
        focusZone()
    else
        paintMsg()
        m.top.setFocus(true)
    end if
end sub

function onKeyboardText(p as object) as dynamic
    return true
end function
