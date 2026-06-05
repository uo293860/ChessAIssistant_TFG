import {useId, useState} from 'react'

type FaqAccordionItemProps = {
  question: string
  answer: string
}

export function FaqAccordionItem({ question, answer }: FaqAccordionItemProps) {
  const [isOpen, setIsOpen] = useState(false)
  const answerId = useId()

  return (
    <article className={`faq-item ${isOpen ? 'open' : ''}`}>
      <button
        type="button"
        className="faq-question"
        aria-expanded={isOpen}
        aria-controls={answerId}
        onClick={() => setIsOpen((currentValue) => !currentValue)}
      >
        <span className="faq-arrow" aria-hidden="true" />
        <span>{question}</span>
      </button>
      {isOpen ? (
        <div className="faq-answer" id={answerId}>
          <p>{answer}</p>
        </div>
      ) : null}
    </article>
  )
}
