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

def getangle(a,b)
  dif = (b[0]-a[0])
  if dif == 0
    dif = 1
  end
  return Math.atan((b[1]-a[1])/(dif))
end

puts xmltag("test","true")

#current directory
this_dir = File.dirname(__FILE__)

#directory for images
subdir = '/data/the friendly'

this_dir << subdir

puts "Tagging all images in #{this_dir}"

files = Dir.entries(this_dir)

puts "#{files.size - 2 } files found"

imnumber = 0

File.open('./xml/imagedata.xml','w') do |f|
  f.puts "<annotations>"
  files.each do |file|
    imnumber += 1
    next if (!file.include? ".jpg")
    f.puts "<image>"
    image = Surface.load("#{this_dir}/#{file}") 
    f.puts xmltag("filename","#{this_dir}/#{file}")
    puts "Loading #{file}, #{imnumber} of #{files.size - 2}"

    @screen = Screen.open( image.size )

    @screen.title = File.basename(file)

    image.blit( @screen, [0,0] )

    @event_queue = Rubygame::EventQueue.new

    @event_queue.enable_new_style_events

    @screen.update

    numclicks = 0

    clickarray = []

    while event = @event_queue.wait
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
