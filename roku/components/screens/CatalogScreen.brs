' Películas o Series: categorías arriba y pósters abajo.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.body = m.top.createChild("Group")
    m.rail = m.top.createChild("Rail")
    m.inRail = false
    m.zone = "grid"
    m.cat = -1
    m.chipIdx = 0
    m.grid = invalid
end sub

sub onActiveChange()
    if m.top.active then
        m.kind = m.top.params.kind
        if m.kind = "MOVIE" then m.rail.current = "peliculas" else m.rail.current = "series"
        st = app("getState")
        if st.profile <> invalid then m.rail.avatar = st.profile.avatar
        m.listName = "tu lista"
        if st.activeList <> invalid then m.listName = st.activeList.name
        markProgress()
        build()
    end if
end sub

' Marca cuánto llevas en lo que dejaste a medias (la barrita del póster).
sub markProgress()
    for each r in app("recents", {kind: "VOD"})
        n = app("channelNode", {id: r.id})
        if n <> invalid then
            frac = 0.0
            if r.dur <> invalid and r.dur > 0 then frac = r.posSec / r.dur
            if not n.hasField("prog") then n.addFields({prog: frac}) else n.prog = frac
        end if
    end for
end sub

sub build()
    c = m.c
    uiClear(m.body)
    title = "Películas"
    if m.kind = "SERIES" then title = "Series"
    m.sec = invalid
    content = m.global.content
    if content <> invalid then
        sid = "sec_movie"
        if m.kind = "SERIES" then sid = "sec_series"
        for i = 0 to content.getChildCount() - 1
            if content.getChild(i).id = sid then m.sec = content.getChild(i)
        end for
    end if
    total = 0
    if m.sec <> invalid then
        for i = 0 to m.sec.getChildCount() - 1
            total = total + m.sec.getChild(i).getChildCount()
        end for
    end if
    if total = 0 then
        m.empty = true
        g = m.body.createChild("Group")
        g.translation = [600, 360]
        icon = "film"
        if m.kind = "SERIES" then icon = "tv"
        uiCircle(g, 312, 0, 96, c.s2)
        uiIcon(g, icon, 336, 24, 48, c.num)
        uiLabel(g, "Tu lista no trae " + LCase(title), 0, 130, 51, c.text, "display", 720, 1, "center")
        uiLabel(g, "Algunas listas solo traen canales en vivo. Si tu proveedor te dio otra lista con " + LCase(title) + ", agrégala.", 0, 204, 27, c.muted, "regular", 720, 3, "center")
        m.top.setFocus(true)
        return
    end if
    m.empty = false
    m.total = total
    t = uiLabel(m.body, title, 168, 40, 63, c.text, "display")
    uiLabel(m.body, total.ToStr() + " en " + m.listName, 168 + t.boundingRect().width + 24, 66, 27, c.muted)

    ' categorías
    m.chipsG = m.body.createChild("Group")
    m.chipsG.translation = [168, 144]
    m.chips = []
    x = 0
    for i = -1 to m.sec.getChildCount() - 1
        label = "Todas"
        if i >= 0 then label = m.sec.getChild(i).title
        g = m.chipsG.createChild("Group")
        g.translation = [x, 0]
        probe = uiLabel(g, label, 0, 0, 24, c.text, "semibold")
        w = probe.boundingRect().width + 48
        g.removeChild(probe)
        ring = uiRing(g, 0, 0, w, 66, 0, true)
        bg = uiPill(g, 0, 0, w, 66, c.s2)
        lb = uiLabel(g, label, 24, 18, 24, c.muted2, "semibold")
        m.chips.Push({ring: ring, bg: bg, label: lb, x: x, w: w})
        x = x + w + 15
    end for

    m.grid = m.body.createChild("MarkupGrid")
    m.grid.itemComponentName = "PosterCard"
    m.grid.numColumns = 6
    m.grid.numRows = 2
    m.grid.itemSize = [246, 408]
    m.grid.itemSpacing = [24, 18]
    m.grid.drawFocusFeedback = false
    m.grid.vertFocusAnimationStyle = "floatingFocus"
    m.grid.translation = [162, 240]
    m.grid.sectionDividerFont = uiFont("display", 30)
    m.grid.sectionDividerTextColor = c.text
    m.grid.sectionDividerHeight = 66
    m.grid.sectionDividerSpacing = 12
    m.grid.sectionDividerLeftOffset = 6
    m.grid.observeField("itemSelected", "onSelect")
    setCat(m.cat)
    paint()
    if m.zone = "grid" then m.grid.setFocus(true) else m.top.setFocus(true)
end sub

sub setCat(i as integer)
    m.cat = i
    if i < 0 then
        m.grid.content = m.sec
    else
        m.grid.content = m.sec.getChild(i)
    end if
    m.grid.jumpToItem = 0
end sub

' Con "Todas" el grid va por secciones; aquí se traduce el índice al canal.
function itemAt(idx as integer) as dynamic
    if m.cat >= 0 then return m.grid.content.getChild(idx)
    for i = 0 to m.sec.getChildCount() - 1
        g = m.sec.getChild(i)
        n = g.getChildCount()
        if idx < n then return g.getChild(idx)
        idx = idx - n
    end for
    return invalid
end function

sub onSelect()
    n = itemAt(m.grid.itemSelected)
    if n = invalid then return
    sfx("ok")
    app("play", {id: n.id})
end sub

sub paint()
    if m.chips = invalid then return
    c = m.c
    m.chipIdx = minInt(m.chipIdx, m.chips.Count() - 1)
    off = 0
    ch = m.chips[m.chipIdx]
    if ch.x + ch.w > 1620 then off = -(ch.x + ch.w - 1620)
    m.chipsG.translation = [168 + off, 144]
    for i = 0 to m.chips.Count() - 1
        it = m.chips[i]
        sel = (i - 1) = m.cat
        f = not m.inRail and m.zone = "chips" and i = m.chipIdx
        it.ring.visible = f
        col = c.s2
        if sel then col = c.azul
        if f and not sel then col = c.s3
        it.bg.blendColor = col
        if sel or f then it.label.color = c.white else it.label.color = c.muted2
    end for
end sub

function minInt(a as integer, b as integer) as integer
    if a < b then return a
    return b
end function

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.inRail then return railKey(key)
    if m.empty then
        if key = "left" then
            railEnter()
            return true
        end if
        return key <> "back"
    end if
    if key = "back" and m.zone = "grid" and m.grid.itemFocused > 5 then
        m.grid.jumpToItem = 0
        return true
    end if
    if m.zone = "chips" then
        if key = "left" then
            if m.chipIdx > 0 then
                m.chipIdx = m.chipIdx - 1
                sfx("tick")
            else
                railEnter()
            end if
            paint()
            return true
        else if key = "right" then
            if m.chipIdx < m.chips.Count() - 1 then
                m.chipIdx = m.chipIdx + 1
                sfx("tick")
                paint()
            end if
            return true
        else if key = "OK" then
            sfx("ok")
            setCat(m.chipIdx - 1)
            m.zone = "grid"
            paint()
            m.grid.setFocus(true)
            return true
        else if key = "down" then
            m.zone = "grid"
            sfx("tick")
            paint()
            m.grid.setFocus(true)
            return true
        end if
        return key = "up"
    end if
    ' el grid no usó la tecla: estamos en una orilla
    if key = "up" then
        m.zone = "chips"
        m.chipIdx = m.cat + 1
        sfx("tick")
        paint()
        m.top.setFocus(true)
        return true
    else if key = "left" then
        railEnter()
        paint()
        m.top.setFocus(true)
        return true
    end if
    return false
end function

sub railLeft()
    paint()
    if not m.empty and m.zone = "grid" then m.grid.setFocus(true) else m.top.setFocus(true)
end sub

function onKeyboardText(p as object) as dynamic
    return true
end function
