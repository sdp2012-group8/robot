#!/usr/bin/ruby

require "rubygems"
require "rubygame"

include Rubygame

def xmltag(tagname, value)
  return "<#{tagname}>\n#{value}\n</#{tagname}>"
end

def midway(a,b)
  return (a+b)/2
end

def todeg(n)
  return (n.to_f/Math::PI)*180
end

def getangle(a,b)
  from = a.dup
  from[1] = 480 - from[1]
  to = b.dup
  to[1] = 480 - to[1]
  notzero = 0.0001
  if from[0] == to[0]
    to[0] = from[0]-notzero
  end
  tan = (to[1]-from[1]).to_f/(to[0]-from[0])
  puts "tangent is #{tan}"
  if to[0] > from[0] and to[1] > from [1]
    angle = todeg(Math.atan(tan))
    puts angle
    return angle
  elsif to[1] < from [1] and to[0] > from[0]
    angle = 360 + todeg(Math.atan(tan))
    puts angle
    return angle
  else
    angle = 180 + todeg(Math.atan(tan))
    puts angle
    return angle
  end
end


#current directory
this_dir = File.dirname(__FILE__)

#directory for images
subdir = '/data/images/friendly2'

this_dir << subdir

puts "Tagging all images in #{this_dir}"

files = Dir.entries(this_dir)

puts "#{files.size - 2 } files found"

imnumber = 0

numfiles = 0
files.each{|file| numfiles+=1 if file.include? ".jpg"}

File.open('./data/tests/friendly2.xml','w') do |f|
  f.puts "<annotations>"
  files.each do |file|
    next if (!file.include? ".jpg")
    imnumber += 1
    f.puts "<image>"
    image = Surface.load("#{this_dir}/#{file}") 
    f.puts xmltag("filename","#{this_dir}/#{file}")
    puts "Loading #{file}, #{imnumber} of #{numfiles}"

    @screen = Screen.open( image.size )

    @screen.title = File.basename(file)

    image.blit( @screen, [0,0] )

    @event_queue = Rubygame::EventQueue.new

    @event_queue.enable_new_style_events

    @screen.update

    numclicks = 0

    clickarray = []

    while event = @event_queue.wait
      if event.is_a? Rubygame::Events::KeyPressed
        if event.key == :space
          clickarray << [-1,-1]
          puts "#{clickarray}"
        end
        if event.key == :backspace
          clickarray.pop
          puts "#{clickarray}"
        end
      end
      if event.is_a? Rubygame::Events::MousePressed
        clickarray << event.pos
        puts "#{clickarray}"
        break if clickarray.length == 5
      end
      break if event.is_a? Rubygame::Events::QuitRequested
    end
    f.puts "<location-data>"
    f.puts xmltag("ball","#{xmltag('x',clickarray[0][0])}\n#{xmltag('y',clickarray[0][1])}")
    f.puts xmltag("bluerobot","#{xmltag('x',midway(clickarray[1][0],clickarray[2][0]))}\n#{xmltag('y',midway(clickarray[1][1],clickarray[2][1]))}\n#{xmltag('angle',getangle(clickarray[1],clickarray[2]))}")
    f.puts xmltag("yellowrobot","#{xmltag('x',midway(clickarray[3][0],clickarray[4][0]))}\n#{xmltag('y',midway(clickarray[3][1],clickarray[4][1]))}\n#{xmltag('angle',getangle(clickarray[3],clickarray[4]))}")
    f.puts "</location-data>"
    f.puts "</image>"

  end
  f.puts "</annotations>"
end

puts "Image tagging complete"
