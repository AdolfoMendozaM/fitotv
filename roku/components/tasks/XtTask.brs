' Lo que se pide a Xtream al abrirlo: las películas o series de una categoría y las temporadas de una serie.
' Devuelve un ContentNode (holder) con todo armado; la pantalla solo lo acomoda.
sub init()
    m.top.functionName = "work"
end sub

sub work()
    p = m.top.params
    c = p.c
    if p.what = "category" then
        r = fetchText(xtUrl(c, p.xtAction, "&category_id=" + p.catId), 60000)
        if not r.ok then
            m.top.result = {ok: false, reqId: p.reqId, message: r.message}
            return
        end if
        holder = CreateObject("roSGNode", "ContentNode")
        movie = p.xtAction = "get_vod_streams"
        for each part in xtParts(r.text)
            o = xtObj(part)
            if o <> invalid then
                if movie then
                    id = anyStr(o.stream_id)
                    if id <> "" then
                        ext = anyStr(o.container_extension)
                        if ext = "" then ext = "mp4"
                        n = holder.createChild("ContentNode")
                        n.id = "xm" + id
                        n.title = anyStr(o.name).Trim()
                        n.url = c.server + "/movie/" + c.user + "/" + c.pass + "/" + id + "." + ext
                        n.streamFormat = streamFormatFor(n.url)
                        n.HDPosterUrl = anyStr(o.stream_icon).Trim()
                        n.addFields({num: 0, group: p.group, kind: "MOVIE", tvgId: "", sid: id, ua: "", ref: "", seriesId: "", season: 0, episode: 0})
                    end if
                else
                    id = anyStr(o.series_id)
                    if id <> "" then
                        n = holder.createChild("ContentNode")
                        n.id = "xs" + id
                        n.title = anyStr(o.name).Trim()
                        n.url = ""
                        n.HDPosterUrl = anyStr(o.cover).Trim()
                        n.addFields({num: 0, group: p.group, kind: "SERIES", tvgId: "", sid: "", ua: "", ref: "", seriesId: "x:" + id, season: 0, episode: 0})
                    end if
                end if
            end if
        end for
        m.top.result = {ok: true, reqId: p.reqId, holder: holder}
    else if p.what = "series" then
        sid = p.seriesId
        if Left(sid, 2) = "x:" then sid = Mid(sid, 3)
        r = fetchText(xtUrl(c, "get_series_info", "&series_id=" + sid), 45000)
        if not r.ok then
            m.top.result = {ok: false, reqId: p.reqId, message: r.message}
            return
        end if
        root = ParseJson(r.text)
        if type(root) <> "roAssociativeArray" then
            m.top.result = {ok: false, reqId: p.reqId, message: "Tu proveedor no mandó las temporadas de esta serie"}
            return
        end if
        info = root.info
        plot = ""
        cover = p.cover
        year = ""
        if type(info) = "roAssociativeArray" then
            plot = anyStr(info.plot)
            if anyStr(info.cover) <> "" then cover = anyStr(info.cover)
            year = Left(anyStr(info.releaseDate), 4)
            if year = "" then year = Left(anyStr(info.release_date), 4)
        end if
        bySeason = {}
        seasonNums = []
        eps = root.episodes
        lists = []
        if type(eps) = "roAssociativeArray" then
            for each k in eps
                lists.Push({key: k, items: eps[k]})
            end for
        else if type(eps) = "roArray" then
            for each grp in eps
                if type(grp) = "roArray" then lists.Push({key: "", items: grp})
            end for
        end if
        for each lst in lists
            if type(lst.items) = "roArray" then
                for each e in lst.items
                    if type(e) = "roAssociativeArray" then
                        id = anyStr(e.id)
                        if id <> "" then
                            s = numOf(e.season)
                            if s = 0 then s = numOf(lst.key)
                            if s = 0 then s = 1
                            key = "s" + s.ToStr()
                            if not bySeason.DoesExist(key) then
                                bySeason[key] = []
                                seasonNums.Push(s)
                            end if
                            ext = anyStr(e.container_extension)
                            if ext = "" then ext = "mp4"
                            img = cover
                            if type(e.info) = "roAssociativeArray" and anyStr(e.info.movie_image) <> "" then img = anyStr(e.info.movie_image)
                            bySeason[key].Push({id: "xe" + id, num: numOf(e.episode_num), title: anyStr(e.title).Trim(), ext: ext, img: img, season: s})
                        end if
                    end if
                end for
            end if
        end for
        seasonNums.Sort()
        holder = CreateObject("roSGNode", "ContentNode")
        holder.id = "info:" + p.seriesId
        for each s in seasonNums
            sn = holder.createChild("ContentNode")
            sn.title = "Temporada " + s.ToStr()
            sn.addFields({season: s})
            list = bySeason["s" + s.ToStr()]
            list.SortBy("num")
            for each e in list
                n = sn.createChild("ContentNode")
                n.id = e.id
                n.title = e.title
                if n.title = "" then n.title = p.showName + " T" + s.ToStr() + " E" + e.num.ToStr()
                n.url = c.server + "/series/" + c.user + "/" + c.pass + "/" + Mid(e.id, 3) + "." + e.ext
                n.streamFormat = streamFormatFor(n.url)
                n.HDPosterUrl = e.img
                n.addFields({num: 0, group: p.showName, kind: "SERIES", tvgId: "", sid: "", ua: "", ref: "", seriesId: p.seriesId, season: s, episode: e.num, epTitle: ""})
            end for
        end for
        m.top.result = {ok: true, reqId: p.reqId, holder: holder, plot: plot, cover: cover, year: year}
    end if
end sub
