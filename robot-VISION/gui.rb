#!/usr/bin/ruby

require 'gosu'

class GameWindow < Gosu::Window
  attr_reader :clickedpoints
  TOP_COLOR = Gosu::Color.new(0xFFFFFF00)
  BOTTOM_COLOR = Gosu::Color.new(0xFF1D4DB5)
  

  def initialize(image)
    super 640, 480, false
    self.caption = "Also Butts"
    @font = Gosu::Font.new(self, Gosu::default_font_name, 20)
    @message = "Click on the center of the ball"
    @clickedpoints = []
    @acceptinginput = true
    @background_image = Gosu::Image.new(self, image, false)
    puts "Please click on the center of the ball"
  end
  def needs_cursor?
    true
  end

  def update
  end

  def toggleinput
    @acceptinginput = !@acceptinginput
  end

  def loadimage
  end

  def draw
    @clickedpoints.each{|p| rectangle_at(p[0],p[1])}
    @background_image.draw(0,0,-1)
    @font.draw("#{@message}", 10, 10, 0, 1.0, 1.0, 0xffffff00)
  end

  def rectangle_at(x,y)
    draw_quad(
      x-2, y-2, TOP_COLOR,
      x-2, y+2, TOP_COLOR,
      x+2, y-2, TOP_COLOR,
      x+2, y+2, TOP_COLOR,
    0)
  end

  def button_down(id)
    case id
      when Gosu::MsLeft
        if @acceptinginput
          @clickedpoints << [mouse_x,mouse_y]
#          puts "clicky at #{mouse_x},#{mouse_y}"
          self.toggleinput
          sleep(0.2)
          if @clickedpoints.size >= 5
            self.close
          else
            @message = "Click on #{["the center of the ball","the black dot on the blue robot","the stem of the 'T' on the blue robot","the black dot on the yellow robot","the stem of the 'T' on the yellow robot"][@clickedpoints.size]}"
            self.toggleinput
          end
        end
    end
  end

  def ballpos
    return @clickedpoints[0]
  end

  def robotangle(robot)
   if robot == "blue"
      front = @clickedpoints[1]
      back = @clickedpoints[2]
    end
    if robot == "yellow"
      front = @clickedpoints[3]
      back = @clickedpoints[4]
    end
    return Math.atan((front[1]-back[1])/(front[0]-back[0]))
  end

  def robotpos(robot)  
    if robot == "blue"
      front = @clickedpoints[1]
      back = @clickedpoints[2]
    end
    if robot == "yellow"
      front = @clickedpoints[3]
      back = @clickedpoints[4]
    end
    return [(front[0]+back[0])/2,(front[1]+back[1])/2]
  end
end

