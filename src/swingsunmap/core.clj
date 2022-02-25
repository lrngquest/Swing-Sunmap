;; sunmap  as a Swing app via Java-interop; based on  presentation code from
;;  github.com/sebastianbenz/clojure-game-of-life
;;  Copyright (C) 2010 Sebastian Benz    Eclipse Public License


(ns swingsunmap.core
  (:import 
    (javax.swing JFrame JPanel Timer)
    (java.awt Color Dimension)
    (java.awt.event ActionListener )
    (java.awt.image BufferedImage)
    (java.io File)
    (javax.imageio ImageIO)
    (java.time Instant ZoneOffset OffsetDateTime)  ) ;; ==> Java 8 reqd.
  (:require [swingsunmap.sunmap :as sm])
  (:gen-class) )


(defn copy-horiz-line "" [dst src fromX w y]
  (let [pixels  (.getRGB src fromX y w 1 nil 0 w)]
    (.setRGB dst fromX y w 1 pixels 0 w) )  )

(defn cpy1 "" [v dst src] (let [[y xa wa] v]
                            (copy-horiz-line dst src xa wa y)  ))

(defn cpy2 "" [v dst src] (let [[y xa wa yb xb wb] v]
                            (copy-horiz-line dst src xa wa y)
                            (copy-horiz-line dst src xb wb y) ) )


(defn sunmap-panel  [  options]
  (let [
        day-map-img      (ImageIO/read (clojure.java.io/resource "worldmap.jpg"))
        night-map-img    (ImageIO/read (clojure.java.io/resource "worldmap_night.jpg"))

        d-width          (.getWidth day-map-img)
        d-height         (.getHeight day-map-img)
        
        panel (proxy [JPanel ActionListener] []  ;; "once"
                (paintComponent [final-graphics] ;; "each" timer tick
                  (let [image    (BufferedImage. d-width d-height
                                                 BufferedImage/TYPE_INT_ARGB)
                        g        (.getGraphics  image)
                        
                        t        (.atOffset (Instant/now ) ZoneOffset/UTC)
                        tasV    [ (.getYear t)
                                  (.getMonthValue t) (.getDayOfMonth t)
                                  (.getHour t) (.getMinute t) (.getSecond t) ]
                        night-v  (sm/get-night-v  (sm/get-jd tasV))  ]
                   
                    (.drawImage g day-map-img 0 0 nil)

                    (doseq [y  (range d-height)] ;; night-rows over day img
                      (case (count (night-v y))
                        0  0 ;; no-op
                        3  (cpy1 (night-v y) image night-map-img)        
                        6  (cpy2 (night-v y) image night-map-img) ) )
                    (.drawImage final-graphics image 0 0 nil)  )  )
                
                (actionPerformed [e]  ;; "each" timer tick
                  (.repaint this) )  ) ]
    (doto panel
      (.setPreferredSize (Dimension. d-width  d-height )) ) ) )


(defn sunmap-frame   [panel]
  (doto (JFrame. "Sunmap")
    (.setDefaultCloseOperation (JFrame/EXIT_ON_CLOSE))
    (.add panel)
    .pack
    .show))


(defn run 
  ([]  
     (run 
               { :speed 300000 }))  ;; 5*60*1000 : 5min
  
  ([  options] 
     (let [panel (sunmap-panel  options)
           frame (sunmap-frame panel)
           timer (Timer. (options :speed) panel)]
       (.start timer) ) )  )

(defn -main [] (run ))


;;Notes
;; * soft-links have been made in project dir for  worldmap{,_night}.jpg
;; * 'sunmap.clj'  same code as  'sunmap.cljs'  but for namespace


