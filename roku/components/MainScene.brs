' Escena principal: guarda los datos, navega entre pantallas y habla con las tareas.

sub init()
    m.root = m.top.findNode("root")
    m.toastLayer = m.top.findNode("toastLayer")
    m.stack = []
    m.data = regRead("data")
    if m.data = invalid then m.data = {profiles: [], lists: [], settings: {sound: true, lastProfileId: "", onboarded: false}}
    if m.data.settings = invalid then m.data.settings = {sound: true}
    if m.data.profiles = invalid then m.data.profiles = []
    if m.data.lists = invalid then m.data.lists = []
    m.pd = {}
    m.profileId = ""
    m.groups = {}
    m.global.sound = m.data.settings.sound <> false

    m.sfx = {}
    for each n in ["tick", "ok", "back", "on", "off", "step", "success", "fitotv"]
        s = m.top.createChild("SoundEffect")
        s.uri = "pkg:/sounds/sfx_" + n + ".wav"
        s.volume = 45
        m.sfx[n] = s
    end for

    m.epg = CreateObject("roSGNode", "EpgTask")
    m.global.addFields({epg: m.epg})
    m.epg.control = "run"

    m.toastTimer = m.top.createChild("Timer")
    m.toastTimer.duration = 3.5
    m.toastTimer.observeField("fire", "hideToast")

    pushScreen("SplashScreen", {})
end sub

' ------------------------------------------------------------------ navegación
sub pushScreen(name as string, params as object)
    if m.stack.Count() > 0 then
        top = m.stack[m.stack.Count() - 1]
        top.active = false
        top.visible = false
    end if
    s = CreateObject("roSGNode", name)
    s.params = params
    m.root.appendChild(s)
    m.stack.Push(s)
    s.active = true
    s.setFocus(true)
end sub

function go(p as object) as dynamic
    params = p.params
    if params = invalid then params = {}
    pushScreen(p.screen, params)
    return true
end function

function back(p as object) as dynamic
    if m.stack.Count() <= 1 then return false
    top = m.stack.Pop()
    top.active = false
    m.root.removeChild(top)
    prev = m.stack[m.stack.Count() - 1]
    prev.visible = true
    prev.active = true
    prev.setFocus(true)
    return true
end function

function reset(p as object) as dynamic
    while m.stack.Count() > 0
        s = m.stack.Pop()
        s.active = false
        m.root.removeChild(s)
    end while
    params = p.params
    if params = invalid then params = {}
    pushScreen(p.screen, params)
    return true
end function

' Menú de la izquierda: vuelve al Inicio y abre la pestaña pedida.
function goTab(p as object) as dynamic
    name = p.name
    if name = "perfil" then
        reset({screen: "ProfilesScreen"})
        return true
    end if
    while m.stack.Count() > 1
        back({})
    end while
    screens = {guia: ["GuideScreen", {}], peliculas: ["CatalogScreen", {kind: "MOVIE"}], series: ["CatalogScreen", {kind: "SERIES"}], buscar: ["SearchScreen", {}], ajustes: ["SettingsScreen", {}]}
    if screens.DoesExist(name) then pushScreen(screens[name][0], screens[name][1])
    return true
end function

function quit(p as object) as dynamic
    m.top.exitApp = true
    return true
end function

function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if key = "back" then
        if m.stack.Count() > 1 then
            playSfx({name: "back"})
            back({})
            return true
        end if
        m.top.exitApp = true
        return true
    end if
    return false
end function

' ------------------------------------------------------------------ sonidos y avisos
function playSfx(p as object) as dynamic
    if not m.global.sound then return false
    s = m.sfx[p.name]
    if s <> invalid then s.control = "play"
    return true
end function

function setSound(p as object) as dynamic
    m.data.settings.sound = p.on
    m.global.sound = p.on
    save()
    return true
end function

function toast(p as object) as dynamic
    uiClear(m.toastLayer)
    w = 760
    tbox = m.toastLayer.createChild("Group")
    tbox.translation = [1920 - w - 72, 72]
    uiBox(tbox, 0, 0, w, 120, "0x12235FF5", 24)
    uiBox(tbox, 0, 0, 8, 120, "0x3DE0BEFF", 4)
    uiLabel(tbox, p.title, 36, 22, 30, "0xF5F7FFFF", "bold", w - 60)
    if p.text <> invalid then uiLabel(tbox, p.text, 36, 66, 24, "0xAEBBEBFF", "regular", w - 60)
    m.toastTimer.control = "stop"
    m.toastTimer.control = "start"
    return true
end function

sub hideToast()
    uiClear(m.toastLayer)
end sub

' ------------------------------------------------------------------ datos
sub save()
    regWrite("data", m.data)
    m.top.dataVersion = m.top.dataVersion + 1
end sub

function profileById(id as string) as dynamic
    for each p in m.data.profiles
        if p.id = id then return p
    end for
    return invalid
end function

function listsOf(pid as string) as object
    out = []
    for each l in m.data.lists
        if l.profileId = pid then out.Push(l)
    end for
    return out
end function

function activeList() as dynamic
    p = profileById(m.profileId)
    if p = invalid then return invalid
    mine = listsOf(p.id)
    for each l in mine
        if l.id = p.activeListId then return l
    end for
    if mine.Count() > 0 then return mine[0]
    return invalid
end function

function pdOf(pid as string) as object
    if not m.pd.DoesExist(pid) then
        d = regRead("pd_" + pid)
        if d = invalid then d = {recents: [], favs: [], lastGroup: {}}
        if d.recents = invalid then d.recents = []
        if d.favs = invalid then d.favs = []
        if d.lastGroup = invalid then d.lastGroup = {}
        m.pd[pid] = d
    end if
    return m.pd[pid]
end function

sub savePd()
    if m.profileId = "" then return
    regWrite("pd_" + m.profileId, pdOf(m.profileId))
end sub

function getState(p as object) as dynamic
    prof = profileById(m.profileId)
    lists = []
    if prof <> invalid then lists = listsOf(prof.id)
    return {
        profiles: m.data.profiles,
        profile: prof,
        lists: lists,
        activeList: activeList(),
        settings: m.data.settings,
        listStatus: m.top.listStatus
    }
end function

function finishOnboarding(p as object) as dynamic
    m.data.settings.onboarded = true
    save()
    return true
end function

' ------------------------------------------------------------------ perfiles
function createProfile(p as object) as dynamic
    id = newId()
    name = p.name.Trim()
    if name = "" then name = "Yo"
    m.data.profiles.Push({id: id, name: name, avatar: p.avatar, kids: p.kids = true, activeListId: "", createdAt: nowSec()})
    save()
    return id
end function

function updateProfile(p as object) as dynamic
    for each pr in m.data.profiles
        if pr.id = p.id then
            if p.name.Trim() <> "" then pr.name = p.name.Trim()
            pr.avatar = p.avatar
            kidsChanged = (pr.kids = true) <> (p.kids = true)
            pr.kids = p.kids = true
            if kidsChanged and pr.id = m.profileId then loadActive()
        end if
    end for
    save()
    return true
end function

function deleteProfile(p as object) as dynamic
    keep = []
    for each pr in m.data.profiles
        if pr.id <> p.id then keep.Push(pr)
    end for
    m.data.profiles = keep
    lists = []
    for each l in m.data.lists
        if l.profileId <> p.id then lists.Push(l)
    end for
    m.data.lists = lists
    regDelete("pd_" + p.id)
    if m.profileId = p.id then m.profileId = ""
    save()
    return true
end function

function startSession(p as object) as dynamic
    m.profileId = p.id
    m.data.settings.lastProfileId = p.id
    pr = profileById(p.id)
    m.global.kids = pr <> invalid and pr.kids = true
    save()
    loadActive()
    return true
end function

' ------------------------------------------------------------------ listas
sub loadActive(quiet = false as boolean)
    l = activeList()
    m.quiet = quiet and m.global.content <> invalid
    if not m.quiet then
        m.global.content = invalid
        m.epg.config = {mode: "none"}
    end if
    if l = invalid then
        m.global.content = invalid
        m.top.listStatus = {state: "none"}
        return
    end if
    if m.quiet then
        m.top.listStatus = {state: "refreshing", name: l.name, id: l.id}
    else
        m.top.listStatus = {state: "loading", name: l.name, id: l.id}
    end if
    runListTask("load", l, invalid)
end sub

sub runListTask(mode as string, l as object, text as dynamic)
    if m.listTask <> invalid then
        m.listTask.unobserveField("result")
        m.listTask.control = "stop"
    end if
    t = CreateObject("roSGNode", "ListTask")
    t.params = {mode: mode, list: l, kids: m.global.kids, text: text}
    t.observeField("result", "onListResult")
    m.listTask = t
    t.control = "run"
end sub

sub onListResult(ev as object)
    r = ev.getData()
    l = r.list
    if r.ok then
        if r.mode = "check" then
            m.data.lists.Push(l)
            for each pr in m.data.profiles
                if pr.id = m.profileId then pr.activeListId = l.id
            end for
        else
            for each x in m.data.lists
                if x.id = l.id then
                    x.channelCount = r.count
                    x.updatedAt = nowSec()
                    x.lastError = ""
                    if r.epgUrl <> "" and (x.epgUrl = invalid or x.epgUrl = "") then x.epgUrl = r.epgUrl
                end if
            end for
        end if
        save()
        m.global.content = r.content
        m.epg.config = epgConfig(l, r.epgUrl)
        st = "ready"
        if r.mode = "check" then st = "added"
        if r.mode = "load" and m.quiet then toast({title: "Lista actualizada", text: l.name + " trae " + r.count.ToStr() + " canales y programas."})
        m.quiet = false
        m.top.listStatus = {state: st, name: l.name, id: l.id, count: r.count, live: r.live, stale: r.stale = true}
    else
        if r.mode <> "check" then
            for each x in m.data.lists
                if x.id = l.id then x.lastError = r.message
            end for
            save()
        end if
        if r.mode = "load" and m.quiet then toast({title: "No se pudo actualizar " + l.name, text: r.message})
        m.quiet = false
        m.top.listStatus = {state: "error", mode: r.mode, name: l.name, id: l.id, message: r.message}
    end if
end sub

function epgConfig(l as object, epgUrl as string) as object
    if l.kind = "XTREAM" then return {mode: "xtream", api: xtreamApi(l.server, l.user, l.pass)}
    u = epgUrl
    if l.epgUrl <> invalid and l.epgUrl <> "" then u = l.epgUrl
    if u <> "" then return {mode: "xmltv", url: u}
    return {mode: "none"}
end function

function addList(p as object) as dynamic
    l = {id: newId(), profileId: m.profileId, name: p.name, kind: p.kind, url: "", server: "", user: "", pass: "", epgUrl: "", addedAt: nowSec(), updatedAt: 0, channelCount: 0, lastError: ""}
    if l.name = invalid or l.name.Trim() = "" then l.name = "Mi lista"
    if p.kind = "URL" then l.url = p.url.Trim()
    if p.kind = "XTREAM" then
        l.server = p.server.Trim()
        l.user = p.user.Trim()
        l.pass = p.pass
    end if
    m.top.listStatus = {state: "checking", name: l.name}
    runListTask("check", l, p.text)
    return true
end function

function setActiveList(p as object) as dynamic
    for each pr in m.data.profiles
        if pr.id = m.profileId then pr.activeListId = p.id
    end for
    save()
    loadActive()
    return true
end function

function removeList(p as object) as dynamic
    keep = []
    for each l in m.data.lists
        if l.id <> p.id then keep.Push(l)
    end for
    m.data.lists = keep
    for each pr in m.data.profiles
        if pr.activeListId = p.id then pr.activeListId = ""
    end for
    fs = CreateObject("roFileSystem")
    fs.Delete("cachefs:/fitotv_" + p.id + ".m3u")
    save()
    loadActive()
    return true
end function

function refreshActive(p as object) as dynamic
    loadActive(p.quiet = true)
    return true
end function

' ------------------------------------------------------------------ grupos, recientes y favoritos
function setGroup(p as object) as dynamic
    l = activeList()
    if l = invalid then return false
    d = pdOf(m.profileId)
    d.lastGroup[l.id] = p.name
    savePd()
    return true
end function

function getGroup(p as object) as dynamic
    l = activeList()
    if l = invalid then return ""
    d = pdOf(m.profileId)
    if d.lastGroup.DoesExist(l.id) then return d.lastGroup[l.id]
    return ""
end function

function channelNode(p as object) as dynamic
    c = m.global.content
    if c = invalid then return invalid
    return c.findNode(p.id)
end function

function nodeToAA(n as object) as object
    return {id: n.id, name: n.title, url: n.url, logo: n.HDPosterUrl, num: n.num, group: n.group, kind: n.kind, tvgId: n.tvgId, sid: n.sid}
end function

function recordRecent(p as object) as dynamic
    l = activeList()
    if l = invalid or m.profileId = "" then return false
    d = pdOf(m.profileId)
    keep = [{id: p.id, listId: l.id, kind: p.kind, at: nowSec(), posSec: p.posSec, dur: p.dur}]
    for each r in d.recents
        if r.id <> p.id and keep.Count() < 24 then keep.Push(r)
    end for
    d.recents = keep
    savePd()
    return true
end function

function forgetRecent(p as object) as dynamic
    if m.profileId = "" then return false
    d = pdOf(m.profileId)
    keep = []
    for each r in d.recents
        if r.id <> p.id then keep.Push(r)
    end for
    d.recents = keep
    savePd()
    return true
end function

function recents(p as object) as dynamic
    l = activeList()
    out = []
    if l = invalid or m.profileId = "" then return out
    d = pdOf(m.profileId)
    for each r in d.recents
        live = r.kind = "LIVE"
        want = (p.kind = "LIVE" and live) or (p.kind = "VOD" and not live)
        if want and r.listId = l.id then
            n = channelNode({id: r.id})
            if n <> invalid then
                a = nodeToAA(n)
                a.posSec = r.posSec
                a.dur = r.dur
                a.at = r.at
                out.Push(a)
            end if
        end if
    end for
    return out
end function

function isFav(p as object) as dynamic
    if m.profileId = "" then return false
    for each f in pdOf(m.profileId).favs
        if f = p.id then return true
    end for
    return false
end function

function toggleFav(p as object) as dynamic
    d = pdOf(m.profileId)
    keep = []
    was = false
    for each f in d.favs
        if f = p.id then
            was = true
        else
            keep.Push(f)
        end if
    end for
    if not was then keep.Unshift(p.id)
    while keep.Count() > 60
        keep.Pop()
    end while
    d.favs = keep
    savePd()
    return not was
end function

function favorites(p as object) as dynamic
    out = []
    if m.profileId = "" then return out
    for each f in pdOf(m.profileId).favs
        n = channelNode({id: f})
        if n <> invalid then out.Push(nodeToAA(n))
    end for
    return out
end function

' Grupos en vivo, con "Favoritos" primero si hay.
function liveGroups(p as object) as dynamic
    out = []
    c = m.global.content
    if c = invalid then return out
    favs = favorites({})
    if favs.Count() > 0 and p.withFavs <> false then
        fn = CreateObject("roSGNode", "ContentNode")
        fn.title = "Favoritos"
        for each f in favs
            n = channelNode({id: f.id})
            if n <> invalid and n.kind = "LIVE" then fn.appendChild(n.clone(false))
        end for
        if fn.getChildCount() > 0 then out.Push({name: "__fav__", label: "Favoritos", count: fn.getChildCount(), node: fn, star: true})
    end if
    live = c.getChild(0)
    for i = 0 to live.getChildCount() - 1
        g = live.getChild(i)
        out.Push({name: g.title, label: g.title, count: g.getChildCount(), node: g, star: false})
    end for
    return out
end function

function search(p as object) as dynamic
    q = fold(p.q.Trim())
    out = []
    c = m.global.content
    if c = invalid or Len(q) < 2 then return out
    for s = 0 to c.getChildCount() - 1
        sec = c.getChild(s)
        for g = 0 to sec.getChildCount() - 1
            grp = sec.getChild(g)
            for i = 0 to grp.getChildCount() - 1
                n = grp.getChild(i)
                if Instr(1, fold(n.title), q) > 0 then
                    out.Push(nodeToAA(n))
                    if out.Count() >= 60 then return out
                end if
            end for
        end for
    end for
    return out
end function

' ------------------------------------------------------------------ reproducir
function play(p as object) as dynamic
    n = channelNode({id: p.id})
    if n = invalid then return false
    resume = 0
    if n.kind <> "LIVE" and p.fromStart <> true then
        d = pdOf(m.profileId)
        for each r in d.recents
            if r.id = n.id and r.posSec <> invalid then resume = r.posSec
        end for
    end if
    if n.kind = "LIVE" then recordRecent({id: n.id, kind: "LIVE", posSec: 0, dur: 0})
    zap = p.zap
    if zap = invalid then zap = n.getParent()
    pushScreen("PlayerScreen", {id: n.id, resume: resume, zap: zap})
    return true
end function
