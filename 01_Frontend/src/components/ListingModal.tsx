import "./ListingModalStyle.css"
import "./style.css"
import { type Listing } from "../App";
import { CalendarClock, CodeXml, HandCoins, KeyRound, X } from "lucide-react";
import { formatEuro } from "./utils.ts";
import {type JSX, useEffect} from "react";

type Props = {
    listing: Listing;
    onClose: () => void;
};

const ListingModal = ({ listing, onClose }: Props) => {
    
    // Prevent body scroll when modal is open
    useEffect(() => {
        document.body.style.overflow = "hidden";
        return () => {
            document.body.style.overflow = "auto";
        };
    }, []);
    
    // Close modal on Escape key press
    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                onClose();
            }
        };
        window.addEventListener("keydown", handleKeyDown);
        return () => {
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [onClose]);
    
    // Handle overlay click to close modal
    const handleOverlayClick = () => {
        onClose();
    };
    
    // Prevent click events from propagating to the overlay
    const handleContentClick = (event: React.MouseEvent) => {
        event.stopPropagation();
    };
    
    // Simpler, more direct approach to handle **text**: patterns
    const renderDescriptionWithBoldParagraphs = (description: string | null) => {
        if (!description) return <p>No description available.</p>;
        
        console.log("Original description:", description); // Debug log
        
        const elements: JSX.Element[] = [];
        let elementKey = 0;
        
        // Split the text by **text**: pattern
        const parts = description.split(/(\*\*[^*]+\*\*\s*:\s*)/);
        
        console.log("Split parts:", parts); // Debug log
        
        for (let i = 0; i < parts.length; i++) {
            const part = parts[i];
            
            if (!part.trim()) continue;
            
            // Check if this part is a **text**: header
            const headerMatch = part.match(/^\*\*([^*]+)\*\*\s*:\s*$/);
            
            if (headerMatch) {
                // This is a header - create bold paragraph with line
                elements.push(
                  <p key={`header-${elementKey++}`} style={{
                      marginTop: '1.5em',
                      marginBottom: '0.5em',
                      fontWeight: 'bold',
                      borderTop: '1px solid #e0e0e0',
                      paddingTop: '1em'
                  }}>
                      {headerMatch[1]}:
                  </p>
                );
            } else {
                // This is regular content - split by double newlines for paragraphs
                const paragraphs = part.split('\n\n').filter(p => p.trim().length > 0);
                
                paragraphs.forEach(paragraph => {
                    elements.push(
                      <p key={`content-${elementKey++}`} style={{ marginBottom: '1em' }}>
                          {renderInlineBold(paragraph.trim())}
                      </p>
                    );
                });
            }
        }
        
        console.log("Generated elements:", elements.length); // Debug log
        
        return (
          <div className="description-content">
              {elements}
          </div>
        );
    };
    
    // Helper function to handle inline bold text (for regular **bold** words in sentences)
    const renderInlineBold = (text: string) => {
        const parts = text.split('**');
        
        return parts.map((part, index) => {
            // Every odd index should be bold (between ** markers)
            if (index % 2 === 1) {
                return <strong key={index}>{part}</strong>;
            }
            return part;
        });
    };
    
    return (
      <div className="modal-overlay" onClick={handleOverlayClick}>
          <div className="modal-content" onClick={handleContentClick}>
              <button className="modal-close" onClick={onClose}>
                  <X size={24} />
              </button>
              
              <div className="card-content">
                  
                  { /*Title*/ }
                  <div>
                      <h2>{listing.title}</h2>
                  </div>
                  
                  { /*Status*/ }
                  <div>
                        <span className={`status-badge ${listing.status.toLowerCase()}`}>
                            {listing.status}
                        </span>
                  </div>
                  
                  { /*Deadline Date*/ }
                  <div>
                      <CalendarClock size={20}/> {listing.deadlineDate}
                  </div>
                  
                  { /*Description*/ }
                  {renderDescriptionWithBoldParagraphs(listing.description)}
                  
                  { /*Industries*/ }
                  {listing.industries && listing.industries.length > 0 && (
                    <div className="tag-section">
                        <span className="tag-section-title">Industries:</span>
                        <div className="industries-tags">
                            {listing.industries.map((industry, index) => (
                              <span
                                key={index}
                                className={`tag-badge industry-tag industry-${index % 3}`}
                              >
                                        <KeyRound size={14} /> {industry}
                                    </span>
                            ))}
                        </div>
                    </div>
                  )}
                  
                  { /* Technologies*/ }
                  {listing.technologies && listing.technologies.length > 0 && (
                    <div className="tag-section">
                        <span className="tag-section-title">Technologies:</span>
                        <div className="technologies-tags">
                            {listing.technologies.map((tech, index) => (
                              <span
                                key={index}
                                className={`tag-badge tech-tag tech-${index % 3}`}
                              >
                                        <KeyRound size={14} /> {tech}
                                    </span>
                            ))}
                        </div>
                    </div>
                  )}
                  
                  { /*Budget*/ }
                  <div className="card-budget">
                      <strong>
                          <HandCoins/> {formatEuro(listing.budget)}
                      </strong>
                  </div>
                  
                  <div className="card-footer">
                        <span className="card-source">
                            <CodeXml size={16}/> {listing.source}
                        </span>
                      
                      <button className="view-listings-button">
                          <a href={listing.url} target="_blank" rel="noopener noreferrer">View Original Listing</a>
                      </button>
                  </div>
              </div>
          </div>
      </div>
    );
}

export default ListingModal;