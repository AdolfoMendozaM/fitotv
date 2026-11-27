' Todos los grupos de la lista; al elegir uno se vuelve con ese grupo.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.title = uiLabel(m.top, "", 144, 84, 69, m.c.text, "display", 1600)
    m.subLb = uiLabel(m.top, "", 144, 176, 30, m.c.muted, "regular", 1600)
    m.grid = m.top.createChild("MarkupGrid")
    m.grid.itemComponentName = "GroupTile"
    m.grid.numColumns = 4
    m.grid.numRows = 4
    m.grid.itemSize = [402, 156]
    m.grid.itemSpacing = [18, 18]
    m.grid.drawFocusFeedback = false
    m.grid.vertFocusAnimationStyle = "floatingFocus"
    m.grid.translation = [138, 264]
    m.grid.observeField("itemSelected", "onSelect")
end sub

sub onActiveChange()
    if not m.top.active then return
    st = app("getState")
    lname = ""
    if st.activeList <> invalid then lname = st.activeList.name
    m.title.text = "Grupos de " + lname
    m.groups = app("liveGroups", {withFavs: true})
    cur = app("getGroup")
    total = 0
    real = 0
    cn = CreateObject("roSGNode", "ContentNode")
    sel = 0
    for i = 0 to m.groups.Count() - 1
        g = m.groups[i]
        if g.name <> "__fav__" then
            total = total + g.count
            real = real + 1
        end if
        it = cn.createChild("ContentNode")
        it.title = g.label
        if g.count = 1 then it.description = "1 canal" else it.description = g.count.ToStr() + " canales"
        if g.star then it.shortDescriptionLine1 = "star_fill" else it.shortDescriptionLine1 = groupIcon(g.name)
        if g.name = cur then
            it.shortDescriptionLine2 = "1"
            sel = i
        else
            it.shortDescriptionLine2 = "0"
        end if
    end for
    m.subLb.text = total.ToStr() + " canales en " + real.ToStr() + " grupos. Elige uno para ver sus canales."
    m.grid.content = cn
    m.grid.jumpToItem = sel
    m.grid.setFocus(true)
end sub

sub onSelect()
    g = m.groups[m.grid.itemSelected]
    if g = invalid then return
    sfx("ok")
    app("setGroup", {name: g.name})
    app("back")
end sub

function onKeyEvent(key as string, press as boolean) as boolean
    return false
end function

function onKeyboardText(p as object) as dynamic
    return true
end function
