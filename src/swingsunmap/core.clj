;; Sunmap -- a Clojure, FP  learning exercise based on:
;;  * SkyviewCafe  4.0.36  (C) 2000-2007 Kerry Shetline  (see sunmap.clj);
;;  * Swing portion:   https://github.com/sebastianbenz/clojure-game-of-life
;;    Copyright (C) 2010 Sebastian Benz    Eclipse Public License


(ns swingsunmap.core
  (:import 
    (javax.swing JFrame JPanel Timer)
    (java.awt Color Dimension  Font)
    (java.awt.event ActionListener )
    (java.awt.image BufferedImage)
    (javax.imageio ImageIO)
    (java.time Instant ZoneOffset OffsetDateTime)  ) ;; ==> Java 8 reqd.
  (:require [clojure.java.io :as cjio])  (:require [swingsunmap.sunmap :as sm])
  (:gen-class) )


(defn copy-horiz-line "" [^BufferedImage dst  ^BufferedImage  src fromX w y]
  (let [pixels  (.getRGB src fromX y w 1 nil 0 w)]
    (.setRGB dst fromX y w 1 pixels 0 w) )  )

(defn cpy1 "" [[y xa wa]  dst src]  (copy-horiz-line dst src xa wa y) )

(defn cpy2 "" [[y xa wa yb xb wb]  dst src]
  (copy-horiz-line dst src xa wa y)  (copy-horiz-line dst src xb wb y)  )


(def time-font (Font. "SansSerif" Font/BOLD  14))

(def app-state (atom { :night-v []}) )
(def secs-per-day 86400)    (def maxcnt 150)   (def ntrvl 2000) ;;in msec

(defn nowAsV "UTC as a vec" []
  (let [t   (.atOffset (Instant/now ) ZoneOffset/UTC) ]
    [ (.getYear t)  (.getMonthValue t) (.getDayOfMonth t)
      (.getHour t)  (.getMinute t)     (.getSecond t) ] ) )

(defn init-state "" []
  (let [now  (nowAsV )  j-d  (sm/get-jd now)  ]
    (swap! app-state assoc :d-t now :tJD j-d :countdown maxcnt
           :night-v (sm/get-night-v j-d)) ) )

(defn update-state "two-phase timer" []
  (let [{:keys [:d-t :tJD :countdown :night-v]} @app-state
        now-d-t     (nowAsV )
        now-JD      (sm/get-jd  now-d-t)
        nxt-cnt     (->> (- now-JD tJD) ( * secs-per-day  ) (- maxcnt  ) )
        nxt-app-st  (if (<= nxt-cnt 1) ;;s/b calculated  TODO
 ;; then update  timestamp countdown _and_ image
                      {:d-t now-d-t   :tJD now-JD   :countdown maxcnt
                       :night-v (sm/get-night-v now-JD)}
 ;; else  update _only_ countdown
                      (assoc @app-state :countdown nxt-cnt) )          ]
    (reset! app-state nxt-app-st)    )  )

;; Add type annotations as needed to stop Java11 reflection warnings.

(defn draw-msg [^sun.java2d.SunGraphics2D g  ^long wd ^long ht  ^String fmtdstr]
  (.setColor g Color/red)    (.setFont g time-font)
  (.drawString g  fmtdstr  (- wd 300) (- ht 20)) )


(defn sunmap-panel  [  options]
  (let [day-map-img      (ImageIO/read (cjio/resource "worldmap.jpg"))
        night-map-img    (ImageIO/read (cjio/resource "worldmap_night.jpg"))
        d-width          (.getWidth day-map-img)
        d-height         (.getHeight day-map-img)
        
        panel (proxy [JPanel ActionListener] []      ;; "once"
                (paintComponent [^sun.java2d.SunGraphics2D final-graphics];;tick
                  (let [image    (BufferedImage. d-width d-height
                                                 BufferedImage/TYPE_INT_ARGB)
                        ^sun.java2d.SunGraphics2D g      (.getGraphics  image)
                        
                        _                 (update-state )
                        night-v           (:night-v @app-state )
                        [y m d h mt s]    (:d-t @app-state)
                        countdown         (:countdown @app-state)      ]
                   
                    (.drawImage g day-map-img 0 0 nil)

                    (doseq [y  (range d-height)] ;; night-rows over day img
                      (case (count (night-v y))
                        0  0 ;; no-op
                        3  (cpy1 (night-v y) image night-map-img)        
                        6  (cpy2 (night-v y) image night-map-img) ) )
                    (.drawImage final-graphics image 0 0 nil)

                    (->> (format  "At UTC %4d-%02d-%02d %2d:%02d:%02d    %3d"
                                  y m d h mt s  (int countdown))
                         (draw-msg final-graphics d-width d-height  ) )    )  )
                
                (actionPerformed [e]  ;; "each" timer tick
                  (.repaint ^JPanel this) )  ) ]
    (doto panel
      (.setPreferredSize (Dimension. d-width  d-height )) ) ) )


(defn sunmap-frame   [^JPanel panel options]
  (doto (JFrame. "Sunmap and countdown to update")
    (.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
    (.add panel)
    .pack
    .show))


(defn run 
  ([]  
     (run        { :speed 5000 }))  ;; 5*1000 : 5sec
  
  ([  options] 
     (let [panel (sunmap-panel  options)
           frame (sunmap-frame panel options)
           timer (Timer. 100 panel)
           -     (.setDelay timer (options :speed))   ]
       ;;(println (.getInitialDelay timer) " " (.getDelay timer))
       (.start timer) ) )  )

(defn -main [] (init-state) (run {:speed 2000}))


;;Notes  working  2018Nov17  type-annotations 2020Feb17  countdown Aug28
;; * soft-links have been made in project dir for  worldmap{,_night}.jpg
;; * 'sunmap.clj'  same code as  'sunmap.cljs'  but for namespace


