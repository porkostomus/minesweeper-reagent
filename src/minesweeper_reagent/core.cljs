(ns ^:figwheel-hooks minesweeper-reagent.core
  (:require
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]))

(def board-width 9)
(def board-height 9)
(def num-mines 18)

(defn rand-positions []
  (shuffle
    (for [i (range board-width)
          j (range board-height)]
      [i j])))

(defn set-mines [] 
  (loop [squares (repeat num-mines 1)]
    (if (= (count squares) (* board-width board-height))
      squares
      (recur (conj squares 0)))))

(defn init-matrix []
  (into {}
        (map vector
             (rand-positions)
             (set-mines))))

(defonce app-state (atom{:matrix (init-matrix)
     :stepped []
     :game-status :in-progress
     :message "Tread lightly..."}))

; mine-detector

(defn mine? [x y]
  (= 1 (get (:matrix @app-state) [x y])))

(defn top-left? [x y n]
  (if (mine? (dec x) (dec y))
    [x y (inc n)]
    [x y n]))

(defn top? [x y n]
  (if (mine? x (dec y))
    [x y (inc n)]
    [x y n]))

(defn top-right? [x y n]
  (if (mine? (inc x) (dec y))
    [x y (inc n)]
    [x y n]))

(defn right? [x y n]
  (if (mine? (inc x) y)
    [x y (inc n)]
    [x y n]))

(defn bottom-right? [x y n]
  (if (mine? (inc x) (inc y))
    [x y (inc n)]
    [x y n]))

(defn bottom? [x y n]
  (if (mine? x (inc y))
    [x y (inc n)]
    [x y n]))

(defn bottom-left? [x y n]
  (if (mine? (dec x) (inc y))
    [x y (inc n)]
    [x y n]))

(defn left? [x y n]
  (if (mine? (dec x) y)
    [x y (inc n)]
    [x y n]))

(defn mine-detector [x y]
  (->> [x y 0]
       (apply top-left?)
       (apply top?)
       (apply top-right?)
       (apply right?)
       (apply bottom-right?)
       (apply bottom?)
       (apply bottom-left?)
       (apply left?)
       last))

; clear squares

(defn step [x y]
  (swap! app-state assoc-in [:stepped]
         (conj (:stepped @app-state)
               [(dec x) (dec y)]
               [x (dec y)]
               [x (inc y)]
               [(dec x) y]
               [(inc x) y] 
               [(inc x) (dec y)]
               [(inc x) (inc y)]
               [(dec x) (inc y)])))

(defn get-app-element []
  (gdom/getElement "app"))

(defn blank [i j]
  [:rect
   {:width 0.9
    :height 0.9
    :fill "grey"
    :x (+ 0.05 i)
    :y (+ 0.05 j)
    :on-click
    (fn blank-click [e]
      (when (= (:game-status @app-state) :in-progress)
        (swap! app-state assoc-in [:stepped]
               (conj (:stepped @app-state) [i j]))
        (if (= 1 (get (:matrix @app-state) [i j]))
          (do (swap! app-state assoc :game-status :dead)
            (swap! app-state assoc :message "Fuck. You blew up.")))))}])

(defn rect-cell
  [x y]
  [:rect.cell
   {:x (+ 0.05 x) :width 0.9
    :y (+ 0.05 y) :height 0.9
    :fill "white"
    :stroke-width 0.025
    :stroke "black"}])

(defn text-cell [x y]
  [:text
   {:x (+ 0.5 x) :width 1
    :y (+ 0.72 y) :height 1
    :text-anchor "middle"
    :font-size 0.6}
   (if (zero? (mine-detector x y))
     ""
     (str (mine-detector x y)))])

(defn cross [i j]
  [:g {:stroke "darkred"
       :stroke-width 0.4
       :stroke-linecap "round"
       :transform
       (str "translate(" (+ 0.5 i) "," (+ 0.5 j) ") "
            "scale(0.3)")}
   [:line {:x1 -1 :y1 -1 :x2 1 :y2 1}]
   [:line {:x1 1 :y1 -1 :x2 -1 :y2 1}]])

(defn clear-squares! []
  (map step (:stepped @app-state)))

(defn render-board []
  (into
    [:svg.board
     {:view-box (str "0 0 " board-width " " board-height)
      :shape-rendering "auto"
      :style {:max-height "500px"}}]
    (for [i (range board-width)
          j (range board-height)]
      [:g
       [rect-cell i j]
       (if (some #{[i j]} (:stepped @app-state))
         (if (= 1 (get (:matrix @app-state) [i j]))
           [cross i j]
           [text-cell i j])      
         [blank i j])])))

(defn minesweeper []
  [:center
   [:h1 (:message @app-state)]
   [:button
    {:on-click
     (fn new-game-click [e]
       (swap! app-state assoc
              :matrix (init-matrix)
              :message "Welcome back"
              :game-status :in-progress
              :stepped []))}
    "Reset"]
   [:div [render-board]]])

(defn mount [el]
  (reagent/render-component [minesweeper] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)