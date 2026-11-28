' Una serie: portada y datos a la izquierda; temporadas y episodios a la derecha.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.body = m.top.createChild("Group")
    m.rail = m.top.createChild("Rail")
    m.rail.current = "series"
    m.inRail = false
    m.seasons = invalid
    m.si = 0
    m.row = 0
    m.top0 = 0
    m.zone = "rows"
    m.VIS = 6
    m.reqN = 0
end sub

sub onActiveChange()
    if not m.top.active then return
    st = app("getState")
    if st.profile <> invalid then m.rail.avatar = st.profile.avatar
    if m.show = invalid then
        m.show = app("channelNode", {id: m.top.params.id})
        if m.show = invalid then
            drawMessage("Esta serie ya no está en la lista", "Puede que tu proveedor la haya quitado.")
            return
        end if
        m.cover = m.show.HDPosterUrl
        m.plot = ""
        m.year = ""
        load()
    else if m.seasons <> invalid then
        refreshProgress()
        drawAll()
    end if
    m.top.setFocus(true)
end sub

sub load()
    s = m.show
    if Left(s.seriesId, 2) = "x:" then
        drawLoading()
        m.reqN = m.reqN + 1
        m.task = CreateObject("roSGNode", "XtTask")
        m.task.params = {what: "series", reqId: m.reqN, c: m.global.content.xt, seriesId: s.seriesId, showName: s.title, cover: s.HDPosterUrl}
        m.task.observeField("result", "onInfo")
        m.task.control = "run"
    else
        ' Lista M3U: los episodios ya están guardados; se acomodan por temporada.
        holder = m.global.content.findNode("eps:" + s.seriesId)
        bySeason = {}
        nums = []
        if holder <> invalid then
            for i = 0 to holder.getChildCount() - 1
                e = holder.getChild(i)
                k = "s" + e.season.ToStr()
                if not bySeason.DoesExist(k) then
                    bySeason[k] = []
                    nums.Push(e.season)
                end if
                bySeason[k].Push({n: e, ep: e.episode})
            end for
        end if
        nums.Sort()
        m.seasons = []
        for each num in nums
            list = bySeason["s" + num.ToStr()]
            list.SortBy("ep")
            zap = CreateObject("roSGNode", "ContentNode")
            for each it in list
                zap.appendChild(it.n.clone(false))
            end for
            name = "Temporada " + num.ToStr()
            if num = 0 then name = "Episodios"
            m.seasons.Push({num: num, name: name, node: zap})
        end for
        pickStartSeason()
        drawAll()
    end if
end sub

sub onInfo()
    r = m.task.result
    if r = invalid or r.reqId <> m.reqN then return
    if not r.ok then
        drawMessage("No pudimos abrir la serie", r.message + "  Presiona OK para intentar otra vez.")
        m.failed = true
        return
    end if
    m.failed = false
    app("addExtra", {node: r.holder})
    if r.cover <> "" then m.cover = r.cover
    m.plot = r.plot
    m.year = r.year
    m.seasons = []
    for i = 0 to r.holder.getChildCount() - 1
        sn = r.holder.getChild(i)
        m.seasons.Push({num: sn.season, name: sn.title, node: sn})
    end for
    pickStartSeason()
    drawAll()
end sub

' Empieza en la temporada del último episodio que viste.
sub refreshProgress()
    m.progress = {}
    for each r in app("recents", {kind: "VOD"})
        if r.dur <> invalid and r.dur > 0 then m.progress[r.id] = r.posSec / r.dur else m.progress[r.id] = 0.02
    end for
end sub

sub pickStartSeason()
    m.si = 0
    m.row = 0
    refreshProgress()
    for i = 0 to m.seasons.Count() - 1
        node = m.seasons[i].node
        for j = 0 to node.getChildCount() - 1
            if m.progress.DoesExist(node.getChild(j).id) then
                m.si = i
                m.row = j
                return
            end if
        end for
    end for
end sub

' ------------------------------------------------------------------ dibujo
sub drawLeft()
    c = m.c
    lg = m.body.createChild("Group")
    lg.translation = [168, 60]
    uiBox(lg, 0, 0, 330, 495, c.s2, 24)
    uiLabel(lg, initials(m.show.title), 0, 210, 60, c.text, "display", 330, 1, "center")
    if m.cover <> "" then uiImage(lg, m.cover, 0, 0, 330, 495, "scaleToZoom")
    uiLabel(lg, m.show.title, 0, 522, 39, c.text, "display", 330, 3)
    parts = []
    if m.year <> "" then parts.Push(m.year)
    if m.seasons <> invalid then
        n = m.seasons.Count()
        eps = 0
        for each s in m.seasons
            eps = eps + s.node.getChildCount()
        end for
        if n = 1 then
            parts.Push("1 temporada")
        else if n > 1 then
            parts.Push(n.ToStr() + " temporadas")
        end if
        if eps = 1 then
            parts.Push("1 episodio")
        else if eps > 1 then
            parts.Push(eps.ToStr() + " episodios")
        end if
    end if
    info = ""
    for i = 0 to parts.Count() - 1
        if i > 0 then info = info + ", "
        info = info + parts[i]
    end for
    uiLabel(lg, info, 0, 680, 23, c.muted, "regular", 330, 2)
    if m.plot <> "" then uiLabel(lg, m.plot, 0, 744, 21, c.muted2, "regular", 330, 8)
end sub

sub drawLoading()
    uiClear(m.body)
    drawLeft()
    g = m.body.createChild("Group")
    g.translation = [1000, 420]
    uiSpinner(g, 160, 0, 80, m.c.menta)
    uiLabel(g, "Cargando temporadas…", 0, 110, 27, m.c.muted, "regular", 400, 1, "center")
end sub

sub drawMessage(title as string, text as string)
    uiClear(m.body)
    if m.show <> invalid then drawLeft()
    g = m.body.createChild("Group")
    g.translation = [600, 400]
    uiLabel(g, title, 0, 0, 45, m.c.text, "display", 1200, 1, "center")
    uiLabel(g, text, 0, 80, 27, m.c.muted, "regular", 1200, 3, "center")
end sub

sub drawAll()
    uiClear(m.body)
    drawLeft()
    c = m.c
    if m.seasons = invalid or m.seasons.Count() = 0 then
        drawMessage("Esta serie todavía no tiene episodios", "Tu proveedor no mandó ninguno. Prueba más tarde.")
        return
    end if
    ' temporadas
    m.chips = []
    m.chipsG = m.body.createChild("Group")
    m.chipsG.translation = [570, 60]
    x = 0
    if m.seasons.Count() > 1 then
        for i = 0 to m.seasons.Count() - 1
            g = m.chipsG.createChild("Group")
            g.translation = [x, 0]
            probe = uiLabel(g, m.seasons[i].name, 0, 0, 24, c.text, "semibold")
            w = probe.boundingRect().width + 54
            g.removeChild(probe)
            ring = uiRing(g, 0, 0, w, 66, 0, true)
            bg = uiPill(g, 0, 0, w, 66, c.s2)
            lb = uiLabel(g, m.seasons[i].name, 27, 18, 24, c.muted2, "semibold")
            m.chips.Push({ring: ring, bg: bg, label: lb, x: x, w: w})
            x = x + w + 15
        end for
    else
        m.zone = "rows"
    end if
    m.rowsG = m.body.createChild("Group")
    drawRows()
end sub

sub drawRows()
    c = m.c
    uiClear(m.rowsG)
    node = m.seasons[m.si].node
    total = node.getChildCount()
    if m.row >= total then m.row = total - 1
    if m.row < 0 then m.row = 0
    if m.row < m.top0 then m.top0 = m.row
    if m.row > m.top0 + m.VIS - 1 then m.top0 = m.row - m.VIS + 1
    m.rowNodes = []
    y0 = 162
    if m.chips.Count() = 0 then y0 = 60
    last = m.top0 + m.VIS - 1
    if last > total - 1 then last = total - 1
    for i = m.top0 to last
        e = node.getChild(i)
        g = m.rowsG.createChild("Group")
        g.translation = [570, y0 + (i - m.top0) * 144]
        ring = uiRing(g, 0, 0, 1290, 132, 21)
        bg = uiBox(g, 0, 0, 1290, 132, c.s1, 21)
        uiBox(g, 15, 15, 182, 102, c.s3, 14)
        if e.HDPosterUrl <> "" then uiImage(g, e.HDPosterUrl, 15, 15, 182, 102, "scaleToZoom")
        pr = m.progress[e.id]
        if pr <> invalid and pr > 0 then
            if pr > 1 then pr = 1
            uiBox(g, 15, 111, 182, 6, "0x0A1747B3", 0)
            uiBox(g, 15, 111, 182 * pr, 6, c.menta, 0)
        end if
        label = "Episodio"
        if e.episode > 0 then label = "Episodio " + e.episode.ToStr()
        uiLabel(g, label, 228, 26, 22, c.num, "semibold", 900)
        uiLabel(g, episodeTitle(e), 228, 62, 27, c.text, "semibold", 980)
        uiIcon(g, "play", 1290 - 72, 48, 36, c.muted)
        m.rowNodes.Push({ring: ring, bg: bg})
    end for
    if total > m.VIS then uiLabel(m.rowsG, (m.row + 1).ToStr() + " de " + total.ToStr(), 570, 1030, 21, c.dim)
    paint()
end sub

function episodeTitle(e as object) as string
    if e.hasField("epTitle") and e.epTitle <> "" then return e.epTitle
    ep = parseEpisode(e.title)
    if ep <> invalid and ep.title <> "" then return ep.title
    if ep <> invalid then return ""
    return e.title
end function

sub paint()
    c = m.c
    if m.chips <> invalid then
        for i = 0 to m.chips.Count() - 1
            ch = m.chips[i]
            sel = i = m.si
            f = not m.inRail and m.zone = "chips" and sel
            ch.ring.visible = f
            col = c.s2
            if sel then col = c.azul
            ch.bg.blendColor = col
            if sel then ch.label.color = c.white else ch.label.color = c.muted2
        end for
        off = 0
        if m.chips.Count() > 0 then
            cur = m.chips[m.si]
            if cur.x + cur.w > 1280 then off = -(cur.x + cur.w - 1280)
        end if
        m.chipsG.translation = [570 + off, 60]
    end if
    if m.rowNodes = invalid then return
    for i = 0 to m.rowNodes.Count() - 1
        f = not m.inRail and m.zone = "rows" and (m.top0 + i) = m.row
        m.rowNodes[i].ring.visible = f
        if f then m.rowNodes[i].bg.blendColor = c.s3 else m.rowNodes[i].bg.blendColor = c.s1
    end for
end sub

' ------------------------------------------------------------------ teclas
function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.inRail then return railKey(key)
    if m.failed = true and key = "OK" then
        sfx("ok")
        load()
        return true
    end if
    if m.seasons = invalid or m.seasons.Count() = 0 then
        if key = "left" then
            railEnter()
            return true
        end if
        return key <> "back"
    end if
    if m.zone = "chips" then
        if key = "left" then
            if m.si > 0 then
                m.si = m.si - 1
                m.row = 0
                m.top0 = 0
                sfx("tick")
                drawRows()
            else
                railEnter()
                paint()
            end if
        else if key = "right" then
            if m.si < m.seasons.Count() - 1 then
                m.si = m.si + 1
                m.row = 0
                m.top0 = 0
                sfx("tick")
                drawRows()
            end if
        else if key = "down" or key = "OK" then
            m.zone = "rows"
            sfx("tick")
            paint()
        else if key = "back" then
            return false
        end if
        return true
    end if
    node = m.seasons[m.si].node
    if key = "up" then
        if m.row > 0 then
            m.row = m.row - 1
            sfx("tick")
            if m.row < m.top0 then drawRows() else paint()
        else if m.chips.Count() > 0 then
            m.zone = "chips"
            sfx("tick")
            paint()
        end if
        return true
    else if key = "down" then
        if m.row < node.getChildCount() - 1 then
            m.row = m.row + 1
            sfx("tick")
            if m.row > m.top0 + m.VIS - 1 then drawRows() else paint()
        end if
        return true
    else if key = "left" then
        railEnter()
        paint()
        return true
    else if key = "OK" or key = "play" then
        e = node.getChild(m.row)
        if e <> invalid then
            sfx("ok")
            app("play", {id: e.id, zap: node})
        end if
        return true
    end if
    return false
end function

sub railLeft()
    paint()
    m.top.setFocus(true)
end sub

function onKeyboardText(p as object) as dynamic
    return true
end function
